package com.bank.core.application.service;

import com.bank.core.application.port.in.TransferFundsUseCase;
import com.bank.core.application.port.out.AccountRepository;
import com.bank.core.application.port.out.IdempotencyPort;
import com.bank.core.application.port.out.OutboxMessageRepository;
import com.bank.core.application.port.out.TransactionRepository;
import com.bank.core.domain.exception.AccountBlockedException;
import com.bank.core.domain.exception.AccountNotFoundException;
import com.bank.core.domain.exception.InsufficientFundsException;
import com.bank.core.domain.model.Account;
import com.bank.core.domain.model.AccountStatus;
import com.bank.core.domain.model.Money;
import com.bank.core.domain.model.OutboxMessage;
import com.bank.core.domain.model.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the {@link TransferFundsUseCase}.
 *
 * <p>This is the most complex use case in the system. It orchestrates the following steps:</p>
 * <ol>
 *   <li>Check Redis for a prior response to the supplied {@code idempotencyKey}; return
 *       the cached result immediately if found (idempotency guarantee).</li>
 *   <li>Load and lock both accounts from Postgres (pessimistic write lock).</li>
 *   <li>Validate account statuses and source balance.</li>
 *   <li>Apply debit/credit mutations on the domain entities.</li>
 *   <li>Persist both updated accounts and a new {@link Transaction} record.</li>
 *   <li>Atomically write an {@link OutboxMessage} to the {@code outbox_messages} table
 *       (Transactional Outbox Pattern — prevents dual-write).</li>
 *   <li>Store the idempotency response in Redis <em>after</em> the Postgres commit.</li>
 * </ol>
 *
 * <p>On any unrecoverable error, the Redis idempotency lock is released so the caller
 * can safely retry.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class TransferFundsService implements TransferFundsUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferFundsService.class);

    /** Micrometer counter name for completed transfers. */
    private static final String METRIC_TRANSFER_COMPLETED = "banking.transfer.completed";

    /** Micrometer counter name for failed transfers. */
    private static final String METRIC_TRANSFER_FAILED = "banking.transfer.failed";

    /** Micrometer counter name for idempotent replays. */
    private static final String METRIC_TRANSFER_IDEMPOTENT = "banking.transfer.idempotent";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final IdempotencyPort idempotencyPort;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${banking.rabbitmq.exchange}")
    private String exchange;

    @Value("${banking.rabbitmq.routing-key.transaction-completed}")
    private String transactionCompletedRoutingKey;

    /**
     * Constructs a {@code TransferFundsService} with all required dependencies.
     *
     * @param accountRepository      account persistence port
     * @param transactionRepository  transaction persistence port
     * @param outboxMessageRepository outbox persistence port
     * @param idempotencyPort        Redis idempotency port
     * @param objectMapper           Jackson serializer for payload and cache building
     * @param meterRegistry          Micrometer registry for business metrics
     */
    public TransferFundsService(AccountRepository accountRepository,
                                TransactionRepository transactionRepository,
                                OutboxMessageRepository outboxMessageRepository,
                                IdempotencyPort idempotencyPort,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.idempotencyPort = idempotencyPort;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The entire operation runs within a single Postgres transaction. If anything
     * fails after the idempotency lock is acquired but before the commit, the lock
     * is released via {@link IdempotencyPort#release(String)} so that the caller can
     * safely retry.</p>
     *
     * @param command the transfer command
     * @return the completed or cached {@link Transaction}
     */
    @Override
    @Transactional
    public Transaction transfer(TransferCommand command) {
        String key = command.idempotencyKey();

        var cached = idempotencyPort.get(key);
        if (cached.isPresent()) {
            log.info("Idempotent replay for key={}", key);
            meterRegistry.counter(METRIC_TRANSFER_IDEMPOTENT).increment();
            return transactionRepository.findByIdempotencyKey(key)
                    .orElseGet(() -> buildTransactionFromCache(cached.get()));
        }

        boolean acquired = idempotencyPort.tryAcquire(key);
        if (!acquired) {
            log.warn("Concurrent request detected for idempotency key={}", key);
            throw new com.bank.core.domain.exception.DuplicateTransactionException(key);
        }

        try {
            Account source = accountRepository.findByIdWithLock(command.sourceAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

            Account destination = accountRepository.findByIdWithLock(command.destinationAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.destinationAccountId()));

            validateAccountStatus(source);
            validateAccountStatus(destination);

            Money amount = Money.of(command.amount(), command.currencyCode());

            if (!source.getBalance().isGreaterThanOrEqualTo(amount)) {
                throw new InsufficientFundsException(source.getId(), source.getBalance(), amount);
            }

            source.debit(amount);
            destination.credit(amount);

            accountRepository.save(source);
            accountRepository.save(destination);

            Transaction transaction = Transaction.completed(
                    key,
                    source.getId(),
                    destination.getId(),
                    amount
            );
            Transaction saved = transactionRepository.save(transaction);

            String payload = buildTransactionPayload(saved);
            OutboxMessage outboxMessage = OutboxMessage.create(exchange, transactionCompletedRoutingKey, payload);
            outboxMessageRepository.save(outboxMessage);

            idempotencyPort.store(key, payload);

            meterRegistry.counter(METRIC_TRANSFER_COMPLETED).increment();
            log.info("Transfer completed: txId={}, from={}, to={}, amount={}",
                    saved.getId(), source.getId(), destination.getId(), amount);

            return saved;

        } catch (Exception ex) {
            idempotencyPort.release(key);
            meterRegistry.counter(METRIC_TRANSFER_FAILED).increment();
            log.error("Transfer failed for idempotency key={}: {}", key, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Validates that the given account is in {@link AccountStatus#ACTIVE} state.
     *
     * @param account the account to validate
     * @throws AccountBlockedException if the account is blocked
     */
    private void validateAccountStatus(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException(account.getId());
        }
    }

    /**
     * Serializes a {@link Transaction} to a JSON string for use as the outbox payload
     * and the idempotency cache entry.
     *
     * @param transaction the transaction to serialize
     * @return the JSON payload string
     */
    private String buildTransactionPayload(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(new TransactionPayload(
                    transaction.getId().toString(),
                    transaction.getSourceAccountId().toString(),
                    transaction.getDestinationAccountId().toString(),
                    transaction.getAmount().amount().toPlainString(),
                    transaction.getAmount().currency().getCurrencyCode(),
                    transaction.getStatus().name(),
                    transaction.getCreatedAt().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction payload", e);
        }
    }

    /**
     * Reconstructs a minimal {@link Transaction} domain object from a cached JSON string.
     *
     * <p>This path is taken when the original {@code Transaction} row is not found in the
     * database but a cached idempotency value is still present in Redis.</p>
     *
     * @param cachedJson the JSON string stored in Redis
     * @return a reconstituted {@link Transaction}
     */
    private Transaction buildTransactionFromCache(String cachedJson) {
        try {
            TransactionPayload payload = objectMapper.readValue(cachedJson, TransactionPayload.class);
            return new Transaction(
                    java.util.UUID.fromString(payload.transactionId()),
                    null,
                    java.util.UUID.fromString(payload.sourceAccountId()),
                    java.util.UUID.fromString(payload.destinationAccountId()),
                    Money.of(new java.math.BigDecimal(payload.amount()), payload.currency()),
                    com.bank.core.domain.model.TransactionStatus.valueOf(payload.status()),
                    null,
                    java.time.Instant.parse(payload.createdAt())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached transaction payload", e);
        }
    }

    /**
     * Internal DTO used for serializing transaction data to/from the outbox payload
     * and the Redis idempotency cache.
     *
     * @param transactionId        the transaction UUID
     * @param sourceAccountId      the source account UUID
     * @param destinationAccountId the destination account UUID
     * @param amount               the transfer amount as a plain string
     * @param currency             the ISO 4217 currency code
     * @param status               the transaction status name
     * @param createdAt            the ISO-8601 creation timestamp
     */
    record TransactionPayload(
            String transactionId,
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String currency,
            String status,
            String createdAt) {}
}
