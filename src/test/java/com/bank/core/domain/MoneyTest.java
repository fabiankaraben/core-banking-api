package com.bank.core.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithBankersRounding() {
        Money m1 = Money.of(new BigDecimal("10.12345"), "USD"); // ends in 5, preceeding digit 4 is even -> 10.1234
        assertEquals(new BigDecimal("10.1234"), m1.amount());

        Money m2 = Money.of(new BigDecimal("10.12355"), "USD"); // ends in 5, preceeding digit 5 is odd -> 10.1236
        assertEquals(new BigDecimal("10.1236"), m2.amount());
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money m1 = Money.of("10.50", "USD");
        Money m2 = Money.of("20.25", "USD");
        Money result = m1.add(m2);
        assertEquals(new BigDecimal("30.7500"), result.amount());
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrency() {
        Money m1 = Money.of("10.50", "USD");
        Money m2 = Money.of("20.25", "EUR");
        assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
    }

    @Test
    void shouldNotAllowNegativeMoney() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-5.00", "USD"));
    }
}
