package com.bank.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money money) {
        checkCurrency(money);
        return new Money(this.amount.add(money.amount), this.currency);
    }

    public Money subtract(Money money) {
        checkCurrency(money);
        return new Money(this.amount.subtract(money.amount), this.currency);
    }

    public boolean isGreaterThanOrEqual(Money money) {
        checkCurrency(money);
        return this.amount.compareTo(money.amount) >= 0;
    }

    private void checkCurrency(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " != " + money.currency);
        }
    }
}
