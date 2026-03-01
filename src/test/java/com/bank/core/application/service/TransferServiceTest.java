package com.bank.core.application.service;

import com.bank.core.application.port.in.TransferUseCase;
import com.bank.core.application.port.out.AccountPort;
import com.bank.core.application.port.out.EventPublisherPort;
import com.bank.core.application.port.out.TransactionPort;
import com.bank.core.domain.Account;
import com.bank.core.domain.AccountException;
import com.bank.core.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountPort accountPort;
    @Mock
    private TransactionPort transactionPort;
    @Mock
    private EventPublisherPort eventPublisherPort;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountPort, transactionPort, eventPublisherPort);
    }

    @Test
    void shouldExecuteTransferSuccessfully() {
        Account source = Account.create("SRC", "USD");
        source.deposit(Money.of("100.00", "USD"));

        Account dest = Account.create("DEST", "USD");

        when(accountPort.findById(source.getId())).thenReturn(Optional.of(source));
        when(accountPort.findById(dest.getId())).thenReturn(Optional.of(dest));

        TransferUseCase.TransferRequest request = new TransferUseCase.TransferRequest(
                source.getId(), dest.getId(), new BigDecimal("50.00"), "USD", "test-key");

        transferService.transfer(request);

        verify(accountPort, times(2)).save(any(Account.class));
        verify(transactionPort, times(1)).save(any());
        verify(eventPublisherPort, times(1)).publishTransactionCompletedEvent(any());
    }

    @Test
    void shouldFailWhenSourceInsufficientFunds() {
        Account source = Account.create("SRC", "USD");
        source.deposit(Money.of("10.00", "USD"));

        Account dest = Account.create("DEST", "USD");

        when(accountPort.findById(source.getId())).thenReturn(Optional.of(source));
        when(accountPort.findById(dest.getId())).thenReturn(Optional.of(dest));

        TransferUseCase.TransferRequest request = new TransferUseCase.TransferRequest(
                source.getId(), dest.getId(), new BigDecimal("50.00"), "USD", "test-key");

        assertThrows(AccountException.InsufficientFundsException.class, () -> transferService.transfer(request));

        verify(accountPort, never()).save(any(Account.class));
        verify(transactionPort, never()).save(any());
        verify(eventPublisherPort, never()).publishTransactionCompletedEvent(any());
    }
}
