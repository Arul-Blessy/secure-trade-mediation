package com.trade.trademediation.util;

import com.trade.trademediation.config.TradeRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void tenPercentOfThousandIsOneHundred() {
        BigDecimal result = Money.percent(new BigDecimal("1000.00"), TradeRules.DEPOSIT_RATE);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void sevenPercentOfThousandIsSeventy() {
        BigDecimal result = Money.percent(new BigDecimal("1000.00"), TradeRules.VICTIM_SHARE_RATE);
        assertThat(result).isEqualByComparingTo("70.00");
    }

    @Test
    void resultIsRoundedToTwoDecimalsHalfUp() {
        // 999.99 x 0.07 = 69.9993, which must round to 70.00
        BigDecimal result = Money.percent(new BigDecimal("999.99"), TradeRules.VICTIM_SHARE_RATE);
        assertThat(result).isEqualByComparingTo("70.00");
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void victimShareAndAgentShareAlwaysAddUpToThePenalty() {
        String[] prices = {"1.00", "99.99", "333.33", "1000.00", "12345.67", "499999.99"};
        for (String p : prices) {
            BigDecimal price = new BigDecimal(p);
            BigDecimal penalty = Money.percent(price, TradeRules.PENALTY_RATE);
            BigDecimal victim = Money.percent(price, TradeRules.VICTIM_SHARE_RATE);
            BigDecimal agent = penalty.subtract(victim);

            assertThat(agent.signum()).as("agent share for price " + p).isGreaterThanOrEqualTo(0);
            assertThat(victim.add(agent)).as("shares for price " + p).isEqualByComparingTo(penalty);
        }
    }
}