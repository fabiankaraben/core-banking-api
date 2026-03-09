package com.bank.core.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount in a specific currency.
 *
 * <p>All arithmetic operations enforce banker's rounding ({@link RoundingMode#HALF_EVEN})
 * and a fixed scale of 4 decimal places, consistent with the {@code NUMERIC(19,4)} column
 * type used in the {@code accounts} and {@code transactions} tables.</p>
 *
 * <p>Floating-point types ({@code float}, {@code double}) are strictly prohibited
 * throughout this codebase; only {@link BigDecimal} is used for financial calculations.</p>
 *
 * @param amount   the monetary amount, always stored with scale 4
 * @param currency the ISO 4217 currency of this amount
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Scale used for all monetary values (4 decimal places). */
    public static final int SCALE = 4;

    /** Rounding mode applied to all fractional calculations (banker's rounding). */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * Canonical constructor. Normalizes the {@code amount} to the standard
     * {@link #SCALE} and {@link #ROUNDING_MODE} and validates that the amount
     * is non-negative.
     *
     * @param amount   the raw monetary amount
     * @param currency the ISO 4217 currency
     * @throws IllegalArgumentException if {@code amount} is negative
     * @throws NullPointerException     if either argument is {@code null}
     */
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(SCALE, ROUNDING_MODE);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monetary amount cannot be negative: " + amount);
        }
    }

    /**
     * Factory method that creates a {@code Money} instance from a {@link BigDecimal}
     * and an ISO 4217 currency code string.
     *
     * @param amount       the monetary amount
     * @param currencyCode the ISO 4217 currency code (e.g., {@code "USD"})
     * @return a new {@code Money} instance
     * @throws IllegalArgumentException if the currency code is unknown or the amount
     *                                  is negative
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Factory method that creates a {@code Money} instance from a plain {@code String}
     * representation of the amount and an ISO 4217 currency code string.
     *
     * @param amount       the monetary amount as a string (e.g., {@code "100.50"})
     * @param currencyCode the ISO 4217 currency code
     * @return a new {@code Money} instance
     */
    public static Money of(String amount, String currencyCode) {
        return of(new BigDecimal(amount), currencyCode);
    }

    /**
     * Returns a new {@code Money} instance representing the sum of this amount
     * and {@code other}.
     *
     * @param other the addend; must share the same currency
     * @return the sum as a new {@code Money} value object
     * @throws IllegalArgumentException if currencies differ
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Returns a new {@code Money} instance representing the difference between
     * this amount and {@code other}.
     *
     * @param other the subtrahend; must share the same currency
     * @return the difference as a new {@code Money} value object
     * @throws IllegalArgumentException if currencies differ or the result would be negative
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount).setScale(SCALE, ROUNDING_MODE);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction would yield a negative balance: " + this.amount + " - " + other.amount);
        }
        return new Money(result, this.currency);
    }

    /**
     * Returns {@code true} if this amount is greater than or equal to {@code other}.
     *
     * @param other the comparand; must share the same currency
     * @return {@code true} if {@code this >= other}
     * @throws IllegalArgumentException if currencies differ
     */
    public boolean isGreaterThanOrEqualTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Returns {@code true} if this amount equals zero.
     *
     * @return {@code true} if the amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Validates that {@code other} has the same currency as this instance.
     *
     * @param other the money to compare currencies with
     * @throws IllegalArgumentException if currencies differ
     */
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    /**
     * Returns a human-readable representation in the form {@code "100.0000 USD"}.
     *
     * @return string representation of this monetary value
     */
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
