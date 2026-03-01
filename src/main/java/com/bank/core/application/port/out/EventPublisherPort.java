package com.bank.core.application.port.out;

import com.bank.core.domain.Transaction;

public interface EventPublisherPort {
    void publishTransactionCompletedEvent(Transaction transaction);
}
