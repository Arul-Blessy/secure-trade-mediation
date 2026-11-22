package com.trade.trademediation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 500) String imageUrl
) {}