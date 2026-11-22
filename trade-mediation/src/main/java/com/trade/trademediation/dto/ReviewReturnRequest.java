package com.trade.trademediation.dto;

import com.trade.trademediation.enums.ReturnDecision;
import jakarta.validation.constraints.NotNull;

public record ReviewReturnRequest(@NotNull ReturnDecision decision) {}