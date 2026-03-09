package com.bank.core.application.port.in;

import com.bank.core.domain.model.Account;
import java.util.UUID;

/**
 * Inbound port defining the contract for querying account information.
 *
 * <p>Provides a read-only view of an account, including its current balance and
 * status. Follows the Query side of the CQRS principle: no state is mutated.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GetAccountUseCase {

    /**
     * Retrieves a bank account by its unique identifier.
     *
     * @param accountId the UUID of the account to retrieve
     * @return the {@link Account} domain entity
     * @throws com.bank.core.domain.exception.AccountNotFoundException if no account
     *         with the given ID exists in the system
     */
    Account getAccount(UUID accountId);
}
