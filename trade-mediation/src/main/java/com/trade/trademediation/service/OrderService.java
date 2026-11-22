package com.trade.trademediation.service;

import com.trade.trademediation.config.TradeRules;
import com.trade.trademediation.dto.OrderResponse;
import com.trade.trademediation.dto.PlaceOrderRequest;
import com.trade.trademediation.entity.Order;
import com.trade.trademediation.entity.Product;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.enums.OrderStatus;
import com.trade.trademediation.enums.PaymentMode;
import com.trade.trademediation.enums.Role;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.OrderRepository;
import com.trade.trademediation.repository.ProductRepository;
import com.trade.trademediation.util.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, Authentication auth) {
        User buyer = currentUserService.get(auth);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This product is no longer available");
        }

        BigDecimal price = product.getPrice();
        BigDecimal deposit = Money.percent(price, TradeRules.DEPOSIT_RATE);
        BigDecimal locked = request.paymentMode() == PaymentMode.PREPAID ? price : deposit;

        Order order = orderRepository.save(Order.builder()
                .buyer(buyer)
                .seller(product.getSeller())
                .product(product)
                .price(price)
                .paymentMode(request.paymentMode())
                .buyerLockedAmount(locked)
                .sellerDeposit(deposit)
                .status(OrderStatus.NOT_YET_DISPATCHED)
                .sellerAgreed(false)
                .agentNotified(false)
                .returnRequested(false)
                .build());

        // If the buyer cannot afford it, this throws and the whole order is rolled back.
        walletService.lock(buyer, locked, order, "ORDER_LOCK");

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse sellerAgree(Long orderId, Authentication auth) {
        User seller = currentUserService.get(auth);
        Order order = findOrderOfSeller(orderId, seller);

        if (order.getStatus() != OrderStatus.NOT_YET_DISPATCHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This order is no longer open");
        }
        if (order.isSellerAgreed()) {
            throw new ApiException(HttpStatus.CONFLICT, "You already agreed to this order");
        }

        walletService.lock(seller, order.getSellerDeposit(), order, "SELLER_DEPOSIT_LOCK");

        order.setSellerAgreed(true);
        order.setAgentNotified(true);   // now the delivery agent can see it
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(Authentication auth) {
        User user = currentUserService.get(auth);
        List<Order> orders = user.getRole() == Role.BUYER
                ? orderRepository.findByBuyerIdOrderByIdDesc(user.getId())
                : orderRepository.findBySellerIdOrderByIdDesc(user.getId());
        return orders.stream().map(OrderResponse::from).toList();
    }

    private Order findOrderOfSeller(Long orderId, User seller) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }
}