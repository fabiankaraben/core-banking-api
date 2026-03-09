package com.bank.core.infrastructure.out.persistence;

import com.bank.core.application.port.out.AccountRepository;
import com.bank.core.domain.model.Account;
import com.bank.core.domain.model.AccountStatus;
import com.bank.core.domain.model.Money;
import com.bank.core.infrastructure.out.persistence.entity.AccountJpaEntity;
import com.bank.core.infrastructure.out.persistence.repository.SpringDataAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter implementing the {@link AccountRepository} port.
 *
 * <p>Translates between the pure domain {@link Account} object and the JPA
 * {@link AccountJpaEntity}, ensuring that infrastructure concerns (column types,
 * JPA annotations) never leak into the domain or application layers.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class AccountPersistenceAdapter implements AccountRepository {

    private final SpringDataAccountRepository springDataRepo;

    /**
     * Constructs an {@code AccountPersistenceAdapter} with the required Spring Data repository.
     *
     * @param springDataRepo the Spring Data JPA repository for account entities
     */
    public AccountPersistenceAdapter(SpringDataAccountRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converts the domain {@link Account} to a {@link AccountJpaEntity} and delegates
     * to Spring Data's {@code save} method. The returned entity is then mapped back
     * to a domain object to reflect any JPA-generated values (e.g., updated version).</p>
     *
     * @param account the account to save
     * @return the saved domain account
     */
    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = toEntity(account);
        AccountJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param accountId the account UUID to look up
     * @return an {@link Optional} containing the domain account, or empty if not found
     */
    @Override
    public Optional<Account> findById(UUID accountId) {
        return springDataRepo.findById(accountId).map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Issues a {@code SELECT FOR UPDATE} query via the custom repository method.</p>
     *
     * @param accountId the account UUID to lock and retrieve
     * @return an {@link Optional} containing the locked domain account, or empty if not found
     */
    @Override
    public Optional<Account> findByIdWithLock(UUID accountId) {
        return springDataRepo.findByIdWithLock(accountId).map(this::toDomain);
    }

    /**
     * Maps a domain {@link Account} to a JPA {@link AccountJpaEntity}.
     *
     * @param account the domain object to convert
     * @return the corresponding JPA entity
     */
    private AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.getId(),
                account.getCustomerId(),
                account.getBalance().amount(),
                account.getCurrency().getCurrencyCode(),
                account.getStatus(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    /**
     * Maps a JPA {@link AccountJpaEntity} to a domain {@link Account}.
     *
     * @param entity the JPA entity to convert
     * @return the corresponding domain object
     */
    private Account toDomain(AccountJpaEntity entity) {
        Money balance = Money.of(entity.getBalance(), entity.getCurrency());
        Currency currency = Currency.getInstance(entity.getCurrency());
        return new Account(
                entity.getId(),
                entity.getCustomerId(),
                balance,
                currency,
                entity.getStatus() != null ? entity.getStatus() : AccountStatus.ACTIVE,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
