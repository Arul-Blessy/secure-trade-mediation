package com.trade.trademediation.dto;

import com.trade.trademediation.entity.Order;
import com.trade.trademediation.enums.OrderStatus;
import com.trade.trademediation.enums.PaymentMode;
import com.trade.trademediation.enums.ReturnDecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String productName,
        String buyer,
        String seller,
        BigDecimal price,
        PaymentMode paymentMode,
        BigDecimal buyerLockedAmount,
        BigDecimal sellerDeposit,
        OrderStatus status,
        boolean sellerAgreed,
        boolean returnRequested,
        ReturnDecision returnDecision,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getProduct().getName(),
                o.getBuyer().getUsername(),
                o.getSeller().getUsername(),
                o.getPrice(),
                o.getPaymentMode(),
                o.getBuyerLockedAmount(),
                o.getSellerDeposit(),
                o.getStatus(),
                o.isSellerAgreed(),
                o.isReturnRequested(),
                o.getReturnDecision(),
                o.getCreatedAt());
    }
}