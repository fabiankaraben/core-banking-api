package com.bank.core.domain;

import com.bank.core.domain.model.Account;
import com.bank.core.domain.model.AccountStatus;
import com.bank.core.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link Account} domain entity.
 *
 * <p>Verifies the account lifecycle, debit/credit semantics, balance invariants,
 * and status transition rules in complete isolation from the persistence layer.</p>
 */
@DisplayName("Account domain entity")
class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.open(UUID.randomUUID(), new BigDecimal("1000.00"), "USD");
    }

    @Nested
    @DisplayName("factory method Account.open")
    class Open {

        @Test
        @DisplayName("creates an ACTIVE account with the specified initial balance")
        void createsActiveAccountWithBalance() {
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getBalance().amount()).isEqualByComparingTo("1000.0000");
            assertThat(account.getBalance().currency().getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("assigns a non-null UUID")
        void assignsUUID() {
            assertThat(account.getId()).isNotNull();
        }

        @Test
        @DisplayName("version is null before first persist")
        void versionIsNullBeforePersist() {
            assertThat(account.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("debit operation")
    class Debit {

        @Test
        @DisplayName("reduces balance by the specified amount")
        void reducesBalance() {
            account.debit(Money.of("400.00", "USD"));
            assertThat(account.getBalance().amount()).isEqualByComparingTo("600.0000");
        }

        @Test
        @DisplayName("allows full balance to be debited")
        void allowsFullDebit() {
            account.debit(Money.of("1000.00", "USD"));
            assertThat(account.getBalance().isZero()).isTrue();
        }

        @Test
        @DisplayName("rejects debit when balance is insufficient")
        void rejectsInsufficientFunds() {
            assertThatThrownBy(() -> account.debit(Money.of("1000.01", "USD")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient funds");
        }

        @Test
        @DisplayName("rejects debit on a blocked account")
        void rejectsDebitOnBlockedAccount() {
            account.block();
            assertThatThrownBy(() -> account.debit(Money.of("100.00", "USD")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Nested
    @DisplayName("credit operation")
    class Credit {

        @Test
        @DisplayName("increases balance by the specified amount")
        void increasesBalance() {
            account.credit(Money.of("500.00", "USD"));
            assertThat(account.getBalance().amount()).isEqualByComparingTo("1500.0000");
        }

        @Test
        @DisplayName("rejects credit on a blocked account")
        void rejectsCreditOnBlockedAccount() {
            account.block();
            assertThatThrownBy(() -> account.credit(Money.of("100.00", "USD")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("block operation")
    class Block {

        @Test
        @DisplayName("transitions account to BLOCKED status")
        void transitionsToBlocked() {
            account.block();
            assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        }

        @Test
        @DisplayName("rejects blocking an already-blocked account")
        void rejectsDoubleBlock() {
            account.block();
            assertThatThrownBy(account::block)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already blocked");
        }
    }
}
