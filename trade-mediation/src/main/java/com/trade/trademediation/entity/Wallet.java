package com.trade.trademediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "regular_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal regularBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "escrow_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal escrowBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "penalties_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal penaltiesTotal = BigDecimal.ZERO;

    @Version
    private Long version;
}