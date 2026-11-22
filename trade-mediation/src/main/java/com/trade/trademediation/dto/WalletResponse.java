package com.trade.trademediation.dto;

import java.math.BigDecimal;

public record WalletResponse(
        String username,
        String role,
        BigDecimal regularBalance,
        BigDecimal escrowBalance,
        BigDecimal penaltiesTotal,
        BigDecimal reputation
) {}