package com.trade.trademediation.service;

import com.trade.trademediation.config.TradeRules;
import com.trade.trademediation.dto.OrderResponse;
import com.trade.trademediation.entity.Order;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.enums.OrderStatus;
import com.trade.trademediation.enums.PaymentMode;
import com.trade.trademediation.enums.ReturnDecision;
import com.trade.trademediation.enums.Role;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.OrderRepository;
import com.trade.trademediation.repository.UserRepository;
import com.trade.trademediation.util.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private static final Set<OrderStatus> CANCELLABLE =
            EnumSet.of(OrderStatus.NOT_YET_DISPATCHED, OrderStatus.IN_TRANSIT);

    private static final Set<OrderStatus> AGENT_DONE = EnumSet.of(
            OrderStatus.DELIVERED, OrderStatus.COMPLETED,
            OrderStatus.CANCELLED_BY_BUYER, OrderStatus.CANCELLED_BY_SELLER,
            OrderStatus.RETURN_APPROVED, OrderStatus.RETURN_REJECTED);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    // ---------- Seller cancels ----------
    @Transactional
    public OrderResponse sellerCancel(Long orderId, Authentication auth) {
        User seller = currentUserService.get(auth);
        Order order = lockOrder(orderId);
        requireSeller(order, seller);
        requireStatus(order, CANCELLABLE, "This order can no longer be cancelled");

        // The buyer always gets everything they locked back.
        walletService.settle(order.getBuyer(), order.getBuyer(),
                order.getBuyerLockedAmount(), order, "SELLER_CANCEL_BUYER_REFUND");

        // If the seller had already put down a deposit, it is forfeited and shared.
        if (order.isSellerAgreed()) {
            penalizeSeller(order, "SELLER_CANCEL_PENALTY");
        }
        decreaseReputation(seller);

        close(order, OrderStatus.CANCELLED_BY_SELLER);
        return OrderResponse.from(order);
    }

    // ---------- Buyer cancels ----------
    @Transactional
    public OrderResponse buyerCancel(Long orderId, Authentication auth) {
        User buyer = currentUserService.get(auth);
        Order order = lockOrder(orderId);
        requireBuyer(order, buyer);
        requireStatus(order, CANCELLABLE, "This order can no longer be cancelled");

        if (order.isSellerAgreed()) {
            penalizeBuyer(order, "BUYER_CANCEL");
            releaseSellerDeposit(order);
            decreaseReputation(buyer);
        } else {
            // Nobody has been harmed yet, so there is no penalty.
            walletService.settle(buyer, buyer, order.getBuyerLockedAmount(), order, "BUYER_CANCEL_REFUND");
        }

        close(order, OrderStatus.CANCELLED_BY_BUYER);
        return OrderResponse.from(order);
    }

    // ---------- Agent: delivery steps ----------
    @Transactional
    public OrderResponse markInTransit(Long orderId) {
        Order order = lockOrder(orderId);
        requireStatus(order, Set.of(OrderStatus.NOT_YET_DISPATCHED), "Order is not waiting for dispatch");
        if (!order.isSellerAgreed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Seller has not agreed to this order yet");
        }
        order.setStatus(OrderStatus.IN_TRANSIT);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse markDelivered(Long orderId) {
        Order order = lockOrder(orderId);
        requireStatus(order, Set.of(OrderStatus.IN_TRANSIT), "Order is not in transit");
        // Money stays in escrow until the buyer confirms receipt or raises a return.
        order.setStatus(OrderStatus.DELIVERED);
        order.setAgentNotified(false);
        return OrderResponse.from(order);
    }

    // ---------- Buyer: happy path ----------
    @Transactional
    public OrderResponse confirmReceipt(Long orderId, Authentication auth) {
        User buyer = currentUserService.get(auth);
        Order order = lockOrder(orderId);
        requireBuyer(order, buyer);
        requireStatus(order, Set.of(OrderStatus.DELIVERED), "Order has not been delivered yet");

        if (order.getPaymentMode() == PaymentMode.PREPAID) {
            walletService.settle(buyer, order.getSeller(), order.getPrice(), order, "SALE_PAYMENT");
        } else {
            walletService.settle(buyer, buyer, order.getBuyerLockedAmount(), order, "COD_LOCK_REFUND");
        }
        releaseSellerDeposit(order);

        close(order, OrderStatus.COMPLETED);
        return OrderResponse.from(order);
    }

    // ---------- Buyer: return ----------
    @Transactional
    public OrderResponse requestReturn(Long orderId, Authentication auth) {
        User buyer = currentUserService.get(auth);
        Order order = lockOrder(orderId);
        requireBuyer(order, buyer);
        requireStatus(order, Set.of(OrderStatus.DELIVERED), "Returns are allowed only after delivery");

        order.setReturnRequested(true);
        order.setReturnDecision(ReturnDecision.PENDING);
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setAgentNotified(true);
        return OrderResponse.from(order);
    }

    // ---------- Agent: decides the return ----------
    @Transactional
    public OrderResponse reviewReturn(Long orderId, ReturnDecision decision) {
        if (decision != ReturnDecision.REASONABLE && decision != ReturnDecision.NOT_REASONABLE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be REASONABLE or NOT_REASONABLE");
        }
        Order order = lockOrder(orderId);
        requireStatus(order, Set.of(OrderStatus.RETURN_REQUESTED), "No pending return request for this order");

        if (decision == ReturnDecision.REASONABLE) {
            // Seller is at fault: buyer gets everything back, seller's deposit is forfeited.
            walletService.settle(order.getBuyer(), order.getBuyer(),
                    order.getBuyerLockedAmount(), order, "RETURN_BUYER_REFUND");
            penalizeSeller(order, "RETURN_SELLER_PENALTY");
            decreaseReputation(order.getSeller());
            close(order, OrderStatus.RETURN_APPROVED);
        } else {
            // Buyer is at fault: buyer pays the penalty, seller's deposit comes back.
            penalizeBuyer(order, "RETURN");
            releaseSellerDeposit(order);
            decreaseReputation(order.getBuyer());
            close(order, OrderStatus.RETURN_REJECTED);
        }
        order.setReturnDecision(decision);
        return OrderResponse.from(order);
    }

    // ---------- Agent: work queue ----------
    @Transactional(readOnly = true)
    public List<OrderResponse> agentNotifications() {
        return orderRepository.findByAgentNotifiedTrueAndStatusNotIn(AGENT_DONE)
                .stream().map(OrderResponse::from).toList();
    }

    // ---------- Shared money rules ----------

    // Buyer penalty = 10% of price. Buyer gets back (locked amount - penalty).
    // Seller receives 7%, the agent receives the remaining 3%.
    private void penalizeBuyer(Order order, String tag) {
        User buyer = order.getBuyer();
        BigDecimal penalty = Money.percent(order.getPrice(), TradeRules.PENALTY_RATE);
        BigDecimal victimShare = Money.percent(order.getPrice(), TradeRules.VICTIM_SHARE_RATE);
        BigDecimal agentShare = penalty.subtract(victimShare);
        BigDecimal refund = order.getBuyerLockedAmount().subtract(penalty);

        walletService.settle(buyer, buyer, refund, order, tag + "_BUYER_REFUND");
        walletService.settle(buyer, order.getSeller(), victimShare, order, tag + "_PENALTY_TO_SELLER");
        walletService.settle(buyer, agent(), agentShare, order, tag + "_PENALTY_TO_AGENT");
        walletService.addPenalty(buyer, penalty);
    }

    // Seller's whole deposit is forfeited: 7% of price to the buyer, the rest to the agent.
    private void penalizeSeller(Order order, String tag) {
        User seller = order.getSeller();
        BigDecimal deposit = order.getSellerDeposit();
        BigDecimal victimShare = Money.percent(order.getPrice(), TradeRules.VICTIM_SHARE_RATE);
        BigDecimal agentShare = deposit.subtract(victimShare);

        walletService.settle(seller, order.getBuyer(), victimShare, order, tag + "_TO_BUYER");
        walletService.settle(seller, agent(), agentShare, order, tag + "_TO_AGENT");
        walletService.addPenalty(seller, deposit);
    }

    private void releaseSellerDeposit(Order order) {
        if (order.isSellerAgreed()) {
            walletService.settle(order.getSeller(), order.getSeller(),
                    order.getSellerDeposit(), order, "SELLER_DEPOSIT_RELEASE");
        }
    }

    private void decreaseReputation(User user) {
        BigDecimal lowered = user.getReputation().subtract(TradeRules.REPUTATION_PENALTY);
        user.setReputation(lowered.max(BigDecimal.ZERO));
    }

    private void close(Order order, OrderStatus status) {
        order.setStatus(status);
        order.setAgentNotified(false);
    }

    private User agent() {
        return userRepository.findFirstByRoleOrderByIdAsc(Role.AGENT)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No delivery agent is configured"));
    }

    private Order lockOrder(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void requireBuyer(Order order, User user) {
        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    private void requireSeller(Order order, User user) {
        if (!order.getSeller().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    private void requireStatus(Order order, Set<OrderStatus> allowed, String message) {
        if (!allowed.contains(order.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }
}