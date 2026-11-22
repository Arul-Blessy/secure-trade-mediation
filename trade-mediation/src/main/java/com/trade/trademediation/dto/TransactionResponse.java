package com.trade.trademediation.dto;

import com.trade.trademediation.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long orderId,
        String bucket,
        BigDecimal amount,
        String reason,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(WalletTransaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getOrder() == null ? null : t.getOrder().getId(),
                t.getBucket().name(),
                t.getAmount(),
                t.getReason(),
                t.getCreatedAt());
    }
}