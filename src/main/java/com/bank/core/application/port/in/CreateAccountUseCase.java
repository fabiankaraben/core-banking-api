package com.bank.core.application.port.in;

import com.bank.core.domain.model.Account;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound port defining the contract for creating a new bank account.
 *
 * <p>In the Hexagonal Architecture, inbound ports are interfaces that define
 * the use cases the application exposes to the outside world. Adapters (e.g.,
 * REST controllers) depend on this interface rather than on a concrete service,
 * keeping the application layer decoupled from delivery mechanisms.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface CreateAccountUseCase {

    /**
     * Command record encapsulating all data required to open a new bank account.
     *
     * @param customerId     the UUID of the customer who will own the account
     * @param initialBalance the opening balance (must be non-negative)
     * @param currencyCode   the ISO 4217 currency code (e.g., {@code "USD"})
     */
    record CreateAccountCommand(UUID customerId, BigDecimal initialBalance, String currencyCode) {}

    /**
     * Opens a new bank account for the specified customer.
     *
     * @param command the command containing all account creation parameters
     * @return the newly created and persisted {@link Account}
     * @throws IllegalArgumentException if the initial balance is negative or the
     *                                  currency code is invalid
     */
    Account createAccount(CreateAccountCommand command);
}
