package com.bank.core.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateActiveAccountWithZeroBalance() {
        Account account = Account.create("CUST-001", "USD");

        assertNotNull(account.getId());
        assertEquals("CUST-001", account.getCustomerReference());
        assertEquals(Account.AccountStatus.ACTIVE, account.getStatus());
        assertEquals(Money.zero("USD"), account.getBalance());
        assertEquals(0L, account.getVersion());
    }

    @Test
    void shouldDepositMoneySuccessfully() {
        Account account = Account.create("CUST-001", "USD");
        account.deposit(Money.of("100.00", "USD"));

        assertEquals(Money.of("100.00", "USD"), account.getBalance());
    }

    @Test
    void shouldWithdrawMoneySuccessfully() {
        Account account = Account.create("CUST-001", "USD");
        account.deposit(Money.of("100.00", "USD"));

        account.withdraw(Money.of("40.00", "USD"));
        assertEquals(Money.of("60.00", "USD"), account.getBalance());
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        Account account = Account.create("CUST-001", "USD");
        account.deposit(Money.of("50.00", "USD"));

        assertThrows(AccountException.InsufficientFundsException.class, () -> {
            account.withdraw(Money.of("60.00", "USD"));
        });
    }
}
