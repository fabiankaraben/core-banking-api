package com.bank.core.application.port.out;

import com.bank.core.domain.Transaction;

public interface TransactionPort {
    Transaction save(Transaction transaction);
}
