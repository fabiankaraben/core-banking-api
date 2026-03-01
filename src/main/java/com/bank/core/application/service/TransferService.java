package com.bank.core.application.service;

import com.bank.core.application.port.in.TransferUseCase;
import com.bank.core.application.port.out.AccountPort;
import com.bank.core.application.port.out.EventPublisherPort;
import com.bank.core.application.port.out.TransactionPort;
import com.bank.core.domain.Account;
import com.bank.core.domain.AccountException;
import com.bank.core.domain.Money;
import com.bank.core.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService implements TransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;
    private final EventPublisherPort eventPublisherPort;

    public TransferService(AccountPort accountPort, TransactionPort transactionPort,
            EventPublisherPort eventPublisherPort) {
        this.accountPort = accountPort;
        this.transactionPort = transactionPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        log.info("Initiating transfer of {} {} from {} to {}", request.amount(), request.currency(),
                request.sourceAccountId(), request.destinationAccountId());

        Account sourceAccount = accountPort.findById(request.sourceAccountId())
                .orElseThrow(() -> new AccountException.AccountNotFoundException(request.sourceAccountId().toString()));

        Account destinationAccount = accountPort.findById(request.destinationAccountId())
                .orElseThrow(
                        () -> new AccountException.AccountNotFoundException(request.destinationAccountId().toString()));

        Money transferAmount = Money.of(request.amount(), request.currency());

        sourceAccount.withdraw(transferAmount);
        destinationAccount.deposit(transferAmount);

        accountPort.save(sourceAccount);
        accountPort.save(destinationAccount);

        Transaction transaction = Transaction.create(sourceAccount.getId(), destinationAccount.getId(), transferAmount);
        transactionPort.save(transaction);

        eventPublisherPort.publishTransactionCompletedEvent(transaction);

        log.info("Transfer completed successfully. Transaction ID: {}", transaction.getId());
    }
}
