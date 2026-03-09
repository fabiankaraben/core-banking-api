package com.bank.core.application.service;

import com.bank.core.application.port.in.CreateAccountUseCase;
import com.bank.core.application.port.out.AccountRepository;
import com.bank.core.domain.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the {@link CreateAccountUseCase}.
 *
 * <p>Coordinates the creation of a new bank account by delegating domain construction
 * to {@link Account#open(java.util.UUID, java.math.BigDecimal, String)} and persisting
 * the result via the {@link AccountRepository} outbound port.</p>
 *
 * <p>The method is wrapped in a {@link Transactional} boundary to ensure the initial
 * balance is atomically written to Postgres.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class CreateAccountService implements CreateAccountUseCase {

    private final AccountRepository accountRepository;

    /**
     * Constructs a {@code CreateAccountService} with the required repository.
     *
     * @param accountRepository the outbound persistence port for accounts
     */
    public CreateAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new {@link Account} domain object via the factory method and
     * persists it within a single database transaction.</p>
     *
     * @param command the account creation command
     * @return the persisted {@link Account}
     */
    @Override
    @Transactional
    public Account createAccount(CreateAccountCommand command) {
        Account account = Account.open(
                command.customerId(),
                command.initialBalance(),
                command.currencyCode()
        );
        return accountRepository.save(account);
    }
}
