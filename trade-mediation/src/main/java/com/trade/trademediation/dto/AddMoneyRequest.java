package com.trade.trademediation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddMoneyRequest(
        @NotNull @DecimalMin("1.00") @DecimalMax("100000.00") BigDecimal amount
) {}