package com.trade.trademediation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {}

    public static BigDecimal percent(BigDecimal base, String rate) {
        return base.multiply(new BigDecimal(rate)).setScale(2, RoundingMode.HALF_UP);
    }
}