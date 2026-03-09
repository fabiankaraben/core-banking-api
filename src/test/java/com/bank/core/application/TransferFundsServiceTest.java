package com.bank.core.application;

import com.bank.core.application.port.in.TransferFundsUseCase;
import com.bank.core.application.port.out.AccountRepository;
import com.bank.core.application.port.out.IdempotencyPort;
import com.bank.core.application.port.out.OutboxMessageRepository;
import com.bank.core.application.port.out.TransactionRepository;
import com.bank.core.application.service.TransferFundsService;
import com.bank.core.domain.exception.AccountNotFoundException;
import com.bank.core.domain.exception.InsufficientFundsException;
import com.bank.core.domain.model.Account;
import com.bank.core.domain.model.Money;
import com.bank.core.domain.model.OutboxMessage;
import com.bank.core.domain.model.Transaction;
import com.bank.core.domain.model.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransferFundsService}.
 *
 * <p>All external dependencies (repositories, Redis port) are replaced with Mockito
 * mocks, ensuring pure in-memory execution without any infrastructure.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferFundsService")
class TransferFundsServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private OutboxMessageRepository outboxMessageRepository;
    @Mock private IdempotencyPort idempotencyPort;

    private TransferFundsService service;

    private UUID sourceId;
    private UUID destId;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        service = new TransferFundsService(
                accountRepository,
                transactionRepository,
                outboxMessageRepository,
                idempotencyPort,
                objectMapper,
                new SimpleMeterRegistry()
        );

        // Use reflection to inject @Value fields
        var exchangeField = TransferFundsService.class.getDeclaredField("exchange");
        exchangeField.setAccessible(true);
        exchangeField.set(service, "banking.events");

        var routingKeyField = TransferFundsService.class.getDeclaredField("transactionCompletedRoutingKey");
        routingKeyField.setAccessible(true);
        routingKeyField.set(service, "transaction.completed");

        sourceId = UUID.randomUUID();
        destId = UUID.randomUUID();
        source = Account.open(UUID.randomUUID(), new BigDecimal("1000.00"), "USD");
        destination = Account.open(UUID.randomUUID(), new BigDecimal("500.00"), "USD");
    }

    @Nested
    @DisplayName("successful transfer")
    class SuccessfulTransfer {

        @Test
        @DisplayName("debits source, credits destination, saves transaction and outbox message")
        void executesTransferSuccessfully() {
            String key = UUID.randomUUID().toString();

            when(idempotencyPort.get(key)).thenReturn(Optional.empty());
            when(idempotencyPort.tryAcquire(key)).thenReturn(true);
            when(accountRepository.findByIdWithLock(source.getId())).thenReturn(Optional.of(source));
            when(accountRepository.findByIdWithLock(destination.getId())).thenReturn(Optional.of(destination));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new TransferFundsUseCase.TransferCommand(
                    key, source.getId(), destination.getId(), new BigDecimal("300.00"), "USD");

            Transaction result = service.transfer(command);

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(source.getBalance().amount()).isEqualByComparingTo("700.0000");
            assertThat(destination.getBalance().amount()).isEqualByComparingTo("800.0000");

            verify(accountRepository).save(source);
            verify(accountRepository).save(destination);
            verify(transactionRepository).save(any());
            verify(outboxMessageRepository).save(any());
            verify(idempotencyPort).store(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("returns cached result on replay without re-processing")
        void returnsCachedResultOnReplay() {
            String key = UUID.randomUUID().toString();
            Transaction existing = Transaction.completed(key, sourceId, destId,
                    Money.of("100.00", "USD"));

            when(idempotencyPort.get(key)).thenReturn(Optional.of("cached-json"));
            when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

            var command = new TransferFundsUseCase.TransferCommand(
                    key, sourceId, destId, new BigDecimal("100.00"), "USD");

            Transaction result = service.transfer(command);

            assertThat(result).isSameAs(existing);
            verify(accountRepository, never()).findByIdWithLock(any());
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws AccountNotFoundException when source account does not exist")
        void throwsWhenSourceMissing() {
            String key = UUID.randomUUID().toString();
            when(idempotencyPort.get(key)).thenReturn(Optional.empty());
            when(idempotencyPort.tryAcquire(key)).thenReturn(true);
            when(accountRepository.findByIdWithLock(any())).thenReturn(Optional.empty());

            var command = new TransferFundsUseCase.TransferCommand(
                    key, sourceId, destId, new BigDecimal("100.00"), "USD");

            assertThatThrownBy(() -> service.transfer(command))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(idempotencyPort).release(key);
        }

        @Test
        @DisplayName("throws InsufficientFundsException when source balance is too low")
        void throwsWhenInsufficientFunds() {
            String key = UUID.randomUUID().toString();
            Account poorSource = Account.open(UUID.randomUUID(), new BigDecimal("10.00"), "USD");

            when(idempotencyPort.get(key)).thenReturn(Optional.empty());
            when(idempotencyPort.tryAcquire(key)).thenReturn(true);
            when(accountRepository.findByIdWithLock(poorSource.getId())).thenReturn(Optional.of(poorSource));
            when(accountRepository.findByIdWithLock(destination.getId())).thenReturn(Optional.of(destination));

            var command = new TransferFundsUseCase.TransferCommand(
                    key, poorSource.getId(), destination.getId(), new BigDecimal("500.00"), "USD");

            assertThatThrownBy(() -> service.transfer(command))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(idempotencyPort).release(key);
        }
    }

    @Nested
    @DisplayName("outbox message")
    class OutboxMessageVerification {

        @Test
        @DisplayName("outbox message contains correct exchange and routing key")
        void outboxMessageHasCorrectMetadata() {
            String key = UUID.randomUUID().toString();
            when(idempotencyPort.get(key)).thenReturn(Optional.empty());
            when(idempotencyPort.tryAcquire(key)).thenReturn(true);
            when(accountRepository.findByIdWithLock(source.getId())).thenReturn(Optional.of(source));
            when(accountRepository.findByIdWithLock(destination.getId())).thenReturn(Optional.of(destination));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new TransferFundsUseCase.TransferCommand(
                    key, source.getId(), destination.getId(), new BigDecimal("100.00"), "USD");

            service.transfer(command);

            ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
            verify(outboxMessageRepository).save(captor.capture());

            OutboxMessage captured = captor.getValue();
            assertThat(captured.getExchange()).isEqualTo("banking.events");
            assertThat(captured.getRoutingKey()).isEqualTo("transaction.completed");
            assertThat(captured.isPublished()).isFalse();
            assertThat(captured.getPayload()).contains(source.getId().toString());
            assertThat(captured.getPayload()).contains(destination.getId().toString());
            assertThat(captured.getPayload()).contains("COMPLETED");
        }
    }
}
