package com.trade.trademediation.service;

import com.trade.trademediation.dto.OrderResponse;
import com.trade.trademediation.entity.Order;
import com.trade.trademediation.entity.Product;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.enums.OrderStatus;
import com.trade.trademediation.enums.PaymentMode;
import com.trade.trademediation.enums.ReturnDecision;
import com.trade.trademediation.enums.Role;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.OrderRepository;
import com.trade.trademediation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Every test uses a 1000.00 product, so the expected numbers are easy to read:
 * deposit/penalty = 100.00, victim share = 70.00, agent share = 30.00.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscrowServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private CurrentUserService currentUserService;
    @Mock private Authentication auth;

    private EscrowService escrowService;

    private User buyer;
    private User seller;
    private User agent;

    @BeforeEach
    void setUp() {
        escrowService = new EscrowService(orderRepository, userRepository,
                walletService, currentUserService);

        buyer = User.builder().id(1L).username("buyer").role(Role.BUYER).build();
        seller = User.builder().id(2L).username("seller").role(Role.SELLER).build();
        agent = User.builder().id(3L).username("agent").role(Role.AGENT).build();

        when(userRepository.findFirstByRoleOrderByIdAsc(Role.AGENT)).thenReturn(Optional.of(agent));
    }

    // ---------- helpers ----------

    private Order order(PaymentMode mode, boolean sellerAgreed, OrderStatus status) {
        BigDecimal price = new BigDecimal("1000.00");
        BigDecimal locked = mode == PaymentMode.PREPAID ? price : new BigDecimal("100.00");
        return Order.builder()
                .id(10L)
                .buyer(buyer)
                .seller(seller)
                .product(Product.builder().name("Headphones").build())
                .price(price)
                .paymentMode(mode)
                .buyerLockedAmount(locked)
                .sellerDeposit(new BigDecimal("100.00"))
                .status(status)
                .sellerAgreed(sellerAgreed)
                .build();
    }

    private void arrange(Order order, User caller) {
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(currentUserService.get(any())).thenReturn(caller);
    }

    // Matches a money amount by value, ignoring how many decimals it was written with.
    private static BigDecimal amt(String value) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(value)) == 0);
    }

    // ---------- buyer cancels ----------

    @Test
    void buyerCancelAfterSellerAgreed_chargesTenPercent_andSplitsSevenAndThree() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, buyer);

        OrderResponse result = escrowService.buyerCancel(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("900.00"), eq(o), eq("BUYER_CANCEL_BUYER_REFUND"));
        verify(walletService).settle(eq(buyer), eq(seller), amt("70.00"), eq(o), eq("BUYER_CANCEL_PENALTY_TO_SELLER"));
        verify(walletService).settle(eq(buyer), eq(agent), amt("30.00"), eq(o), eq("BUYER_CANCEL_PENALTY_TO_AGENT"));
        verify(walletService).addPenalty(eq(buyer), amt("100.00"));
        verify(walletService).settle(eq(seller), eq(seller), amt("100.00"), eq(o), eq("SELLER_DEPOSIT_RELEASE"));

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED_BY_BUYER);
        assertThat(buyer.getReputation()).isEqualByComparingTo("9.9");
    }

    @Test
    void buyerCancelBeforeSellerAgreed_isFreeAndKeepsReputation() {
        Order o = order(PaymentMode.PREPAID, false, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, buyer);

        escrowService.buyerCancel(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("1000.00"), eq(o), eq("BUYER_CANCEL_REFUND"));
        verifyNoMoreInteractions(walletService);
        assertThat(buyer.getReputation()).isEqualByComparingTo("10.0");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED_BY_BUYER);
    }

    @Test
    void buyerCancelOnCodAfterAgreement_getsNothingBack() {
        // COD locks only 10% (100.00), and the penalty is also 10% (100.00).
        Order o = order(PaymentMode.COD, true, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, buyer);

        escrowService.buyerCancel(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("0.00"), eq(o), eq("BUYER_CANCEL_BUYER_REFUND"));
        verify(walletService).settle(eq(buyer), eq(seller), amt("70.00"), eq(o), eq("BUYER_CANCEL_PENALTY_TO_SELLER"));
        verify(walletService).settle(eq(buyer), eq(agent), amt("30.00"), eq(o), eq("BUYER_CANCEL_PENALTY_TO_AGENT"));
    }

    // ---------- seller cancels ----------

    @Test
    void sellerCancelAfterAgreeing_forfeitsDeposit_sevenToBuyer_threeToAgent() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, seller);

        OrderResponse result = escrowService.sellerCancel(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("1000.00"), eq(o), eq("SELLER_CANCEL_BUYER_REFUND"));
        verify(walletService).settle(eq(seller), eq(buyer), amt("70.00"), eq(o), eq("SELLER_CANCEL_PENALTY_TO_BUYER"));
        verify(walletService).settle(eq(seller), eq(agent), amt("30.00"), eq(o), eq("SELLER_CANCEL_PENALTY_TO_AGENT"));
        verify(walletService).addPenalty(eq(seller), amt("100.00"));

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED_BY_SELLER);
        assertThat(seller.getReputation()).isEqualByComparingTo("9.9");
    }

    @Test
    void sellerCancelBeforeAgreeing_hasNoMoneyPenalty_butLosesReputation() {
        Order o = order(PaymentMode.PREPAID, false, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, seller);

        escrowService.sellerCancel(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("1000.00"), eq(o), eq("SELLER_CANCEL_BUYER_REFUND"));
        verifyNoMoreInteractions(walletService);
        assertThat(seller.getReputation()).isEqualByComparingTo("9.9");
    }

    // ---------- returns ----------

    @Test
    void returnJudgedReasonable_refundsBuyer_andPenalizesSeller() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.RETURN_REQUESTED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(o));

        OrderResponse result = escrowService.reviewReturn(10L, ReturnDecision.REASONABLE);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("1000.00"), eq(o), eq("RETURN_BUYER_REFUND"));
        verify(walletService).settle(eq(seller), eq(buyer), amt("70.00"), eq(o), eq("RETURN_SELLER_PENALTY_TO_BUYER"));
        verify(walletService).settle(eq(seller), eq(agent), amt("30.00"), eq(o), eq("RETURN_SELLER_PENALTY_TO_AGENT"));
        verify(walletService).addPenalty(eq(seller), amt("100.00"));

        assertThat(result.status()).isEqualTo(OrderStatus.RETURN_APPROVED);
        assertThat(result.returnDecision()).isEqualTo(ReturnDecision.REASONABLE);
        assertThat(seller.getReputation()).isEqualByComparingTo("9.9");
        assertThat(buyer.getReputation()).isEqualByComparingTo("10.0");
    }

    @Test
    void returnJudgedNotReasonable_penalizesBuyer_andReleasesSellerDeposit() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.RETURN_REQUESTED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(o));

        OrderResponse result = escrowService.reviewReturn(10L, ReturnDecision.NOT_REASONABLE);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("900.00"), eq(o), eq("RETURN_BUYER_REFUND"));
        verify(walletService).settle(eq(buyer), eq(seller), amt("70.00"), eq(o), eq("RETURN_PENALTY_TO_SELLER"));
        verify(walletService).settle(eq(buyer), eq(agent), amt("30.00"), eq(o), eq("RETURN_PENALTY_TO_AGENT"));
        verify(walletService).addPenalty(eq(buyer), amt("100.00"));
        verify(walletService).settle(eq(seller), eq(seller), amt("100.00"), eq(o), eq("SELLER_DEPOSIT_RELEASE"));

        assertThat(result.status()).isEqualTo(OrderStatus.RETURN_REJECTED);
        assertThat(buyer.getReputation()).isEqualByComparingTo("9.9");
        assertThat(seller.getReputation()).isEqualByComparingTo("10.0");
    }

    // ---------- happy path ----------

    @Test
    void confirmReceiptOnPrepaid_paysSeller_andReturnsDeposit() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.DELIVERED);
        arrange(o, buyer);

        OrderResponse result = escrowService.confirmReceipt(10L, auth);

        verify(walletService).settle(eq(buyer), eq(seller), amt("1000.00"), eq(o), eq("SALE_PAYMENT"));
        verify(walletService).settle(eq(seller), eq(seller), amt("100.00"), eq(o), eq("SELLER_DEPOSIT_RELEASE"));
        assertThat(result.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void confirmReceiptOnCod_returnsTheBuyersLock_andPaysNoOneThePrice() {
        Order o = order(PaymentMode.COD, true, OrderStatus.DELIVERED);
        arrange(o, buyer);

        escrowService.confirmReceipt(10L, auth);

        verify(walletService).settle(eq(buyer), eq(buyer), amt("100.00"), eq(o), eq("COD_LOCK_REFUND"));
        verify(walletService).settle(eq(seller), eq(seller), amt("100.00"), eq(o), eq("SELLER_DEPOSIT_RELEASE"));
        verify(walletService, never()).settle(eq(buyer), eq(seller), any(), any(), eq("SALE_PAYMENT"));
    }

    // ---------- rules that must be refused ----------

    @Test
    void cannotCancelAnOrderThatIsAlreadyDelivered() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.DELIVERED);
        arrange(o, buyer);

        assertThatThrownBy(() -> escrowService.buyerCancel(10L, auth))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(walletService);
    }

    @Test
    void cannotMarkDeliveredBeforeItIsInTransit() {
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.NOT_YET_DISPATCHED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> escrowService.markDelivered(10L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void cannotDispatchBeforeTheSellerAgrees() {
        Order o = order(PaymentMode.PREPAID, false, OrderStatus.NOT_YET_DISPATCHED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> escrowService.markInTransit(10L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void aStrangerCannotCancelSomeoneElsesOrder() {
        User stranger = User.builder().id(99L).username("stranger").role(Role.BUYER).build();
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, stranger);

        assertThatThrownBy(() -> escrowService.buyerCancel(10L, auth))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        verifyNoInteractions(walletService);
    }

    @Test
    void reputationNeverGoesBelowZero() {
        buyer.setReputation(new BigDecimal("0.0"));
        Order o = order(PaymentMode.PREPAID, true, OrderStatus.NOT_YET_DISPATCHED);
        arrange(o, buyer);

        escrowService.buyerCancel(10L, auth);

        assertThat(buyer.getReputation()).isEqualByComparingTo("0.0");
    }
}