package com.bank.core.infrastructure.out.persistence;

import com.bank.core.application.port.out.TransactionRepository;
import com.bank.core.domain.model.Money;
import com.bank.core.domain.model.Transaction;
import com.bank.core.infrastructure.out.persistence.entity.TransactionJpaEntity;
import com.bank.core.infrastructure.out.persistence.repository.SpringDataTransactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter implementing the {@link TransactionRepository} port.
 *
 * <p>Translates between the pure domain {@link Transaction} object and the JPA
 * {@link TransactionJpaEntity}. Transactions are append-only; no update logic exists.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository springDataRepo;

    /**
     * Constructs a {@code TransactionPersistenceAdapter}.
     *
     * @param springDataRepo the Spring Data JPA repository for transaction entities
     */
    public TransactionPersistenceAdapter(SpringDataTransactionRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    /**
     * {@inheritDoc}
     *
     * @param transaction the transaction to persist
     * @return the saved domain transaction
     */
    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = toEntity(transaction);
        TransactionJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param transactionId the UUID to look up
     * @return an {@link Optional} containing the domain transaction, or empty
     */
    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        return springDataRepo.findById(transactionId).map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * @param idempotencyKey the key to search by
     * @return an {@link Optional} containing the matching domain transaction, or empty
     */
    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return springDataRepo.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    /**
     * Maps a domain {@link Transaction} to a JPA {@link TransactionJpaEntity}.
     *
     * @param t the domain transaction
     * @return the corresponding JPA entity
     */
    private TransactionJpaEntity toEntity(Transaction t) {
        return new TransactionJpaEntity(
                t.getId(),
                t.getIdempotencyKey(),
                t.getSourceAccountId(),
                t.getDestinationAccountId(),
                t.getAmount().amount(),
                t.getAmount().currency().getCurrencyCode(),
                t.getStatus(),
                t.getFailureReason(),
                t.getCreatedAt()
        );
    }

    /**
     * Maps a JPA {@link TransactionJpaEntity} to a domain {@link Transaction}.
     *
     * @param e the JPA entity
     * @return the corresponding domain transaction
     */
    private Transaction toDomain(TransactionJpaEntity e) {
        Money amount = Money.of(e.getAmount(), e.getCurrency());
        return new Transaction(
                e.getId(),
                e.getIdempotencyKey(),
                e.getSourceAccountId(),
                e.getDestinationAccountId(),
                amount,
                e.getStatus(),
                e.getFailureReason(),
                e.getCreatedAt()
        );
    }
}
