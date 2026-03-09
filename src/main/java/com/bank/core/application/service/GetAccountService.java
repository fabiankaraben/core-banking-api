package com.bank.core.application.service;

import com.bank.core.application.port.in.GetAccountUseCase;
import com.bank.core.application.port.out.AccountRepository;
import com.bank.core.domain.exception.AccountNotFoundException;
import com.bank.core.domain.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing the {@link GetAccountUseCase}.
 *
 * <p>Retrieves an {@link Account} from the persistence layer and surfaces a typed
 * {@link AccountNotFoundException} when the account does not exist, which the REST
 * adapter maps to an HTTP 404 response.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class GetAccountService implements GetAccountUseCase {

    private final AccountRepository accountRepository;

    /**
     * Constructs a {@code GetAccountService} with the required repository.
     *
     * @param accountRepository the outbound persistence port for accounts
     */
    public GetAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This operation is read-only; it is wrapped in a read-only transaction
     * to hint to the persistence provider that no dirty-checking is required,
     * improving performance under high query load.</p>
     *
     * @param accountId the UUID of the account to retrieve
     * @return the {@link Account} domain entity
     * @throws AccountNotFoundException if no account with the given ID exists
     */
    @Override
    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
