package com.trade.trademediation.dto;

import com.trade.trademediation.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull Long productId,
        @NotNull PaymentMode paymentMode
) {}