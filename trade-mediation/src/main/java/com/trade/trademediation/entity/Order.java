package com.trade.trademediation.entity;

import com.trade.trademediation.enums.OrderStatus;
import com.trade.trademediation.enums.PaymentMode;
import com.trade.trademediation.enums.ReturnDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_mode", nullable = false, length = 10)
    private PaymentMode paymentMode;

    @Column(name = "buyer_locked_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal buyerLockedAmount;

    @Column(name = "seller_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellerDeposit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "seller_agreed", nullable = false)
    private boolean sellerAgreed;

    @Column(name = "agent_notified", nullable = false)
    private boolean agentNotified;

    @Column(name = "return_requested", nullable = false)
    private boolean returnRequested;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "return_decision", length = 20)
    private ReturnDecision returnDecision;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}