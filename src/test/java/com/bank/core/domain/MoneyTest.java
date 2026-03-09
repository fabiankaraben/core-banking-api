package com.bank.core.domain;

import com.bank.core.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link Money} value object.
 *
 * <p>Verifies arithmetic operations, rounding behaviour (banker's rounding),
 * currency mismatch detection, and construction invariants.</p>
 */
@DisplayName("Money value object")
class MoneyTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("normalises amount to scale 4")
        void normalisesScale() {
            Money money = Money.of("100", "USD");
            assertThat(money.amount().scale()).isEqualTo(4);
            assertThat(money.amount()).isEqualByComparingTo("100.0000");
        }

        @Test
        @DisplayName("rejects negative amounts")
        void rejectsNegativeAmount() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01"), "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("accepts zero amount")
        void acceptsZeroAmount() {
            Money zero = Money.of(BigDecimal.ZERO, "USD");
            assertThat(zero.isZero()).isTrue();
        }
    }

    @Nested
    @DisplayName("addition")
    class Addition {

        @Test
        @DisplayName("adds two amounts of the same currency")
        void addsSameCurrency() {
            Money a = Money.of("100.00", "USD");
            Money b = Money.of("50.00", "USD");
            assertThat(a.add(b).amount()).isEqualByComparingTo("150.0000");
        }

        @Test
        @DisplayName("rejects addition of different currencies")
        void rejectsCurrencyMismatch() {
            Money usd = Money.of("100", "USD");
            Money eur = Money.of("100", "EUR");
            assertThatThrownBy(() -> usd.add(eur))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }

    @Nested
    @DisplayName("subtraction")
    class Subtraction {

        @Test
        @DisplayName("subtracts smaller from larger amount")
        void subtractsCorrectly() {
            Money a = Money.of("200.00", "USD");
            Money b = Money.of("75.50", "USD");
            assertThat(a.subtract(b).amount()).isEqualByComparingTo("124.5000");
        }

        @Test
        @DisplayName("rejects subtraction that would yield negative result")
        void rejectsNegativeResult() {
            Money a = Money.of("50.00", "USD");
            Money b = Money.of("100.00", "USD");
            assertThatThrownBy(() -> a.subtract(b))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative balance");
        }
    }

    @Nested
    @DisplayName("banker's rounding (HALF_EVEN)")
    class BankersRounding {

        @Test
        @DisplayName("rounds 0.000050 to 0.0000 — half-to-even rounds down when preceding digit is 0 (even)")
        void roundsHalfToEven_roundDown() {
            // HALF_EVEN: 0.000050 is halfway between 0.0000 and 0.0001;
            // the 4th decimal is 0 (even) so it rounds DOWN to 0.0000.
            Money m = Money.of(new BigDecimal("0.000050"), "USD");
            assertThat(m.amount()).isEqualByComparingTo("0.0000");
        }

        @Test
        @DisplayName("toString includes plain amount and currency code")
        void toStringFormat() {
            Money m = Money.of("99.99", "EUR");
            assertThat(m.toString()).isEqualTo("99.9900 EUR");
        }
    }

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("isGreaterThanOrEqualTo returns true when equal")
        void greaterOrEqualWhenEqual() {
            Money a = Money.of("100", "USD");
            Money b = Money.of("100", "USD");
            assertThat(a.isGreaterThanOrEqualTo(b)).isTrue();
        }

        @Test
        @DisplayName("isGreaterThanOrEqualTo returns false when less than")
        void notGreaterOrEqual() {
            Money a = Money.of("99", "USD");
            Money b = Money.of("100", "USD");
            assertThat(a.isGreaterThanOrEqualTo(b)).isFalse();
        }
    }
}
