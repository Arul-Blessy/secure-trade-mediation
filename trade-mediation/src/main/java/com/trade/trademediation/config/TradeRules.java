package com.trade.trademediation.config;

import java.math.BigDecimal;

public final class TradeRules {

    private TradeRules() {}

    public static final String DEPOSIT_RATE = "0.10";        // seller deposit and COD lock
    public static final String PENALTY_RATE = "0.10";        // fraud penalty
    public static final String VICTIM_SHARE_RATE = "0.07";   // victim compensation
    // agent share = penalty - victim share (the 3%)

    public static final BigDecimal MIN_SELLER_REPUTATION = new BigDecimal("5.0");
    public static final BigDecimal REPUTATION_PENALTY = new BigDecimal("0.1");
}