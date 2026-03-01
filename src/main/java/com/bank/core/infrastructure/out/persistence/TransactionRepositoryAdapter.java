package com.bank.core.infrastructure.out.persistence;

import com.bank.core.application.port.out.TransactionPort;
import com.bank.core.domain.Money;
import com.bank.core.domain.Transaction;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepositoryAdapter implements TransactionPort {

    private final JpaTransactionRepository jpaTransactionRepository;

    public TransactionRepositoryAdapter(JpaTransactionRepository jpaTransactionRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = TransactionJpaEntity.builder()
                .id(transaction.getId())
                .sourceAccountId(transaction.getSourceAccountId())
                .destinationAccountId(transaction.getDestinationAccountId())
                .amount(transaction.getAmount().amount())
                .currency(transaction.getAmount().currency())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();

        TransactionJpaEntity savedEntity = jpaTransactionRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    private Transaction mapToDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                Transaction.TransactionStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
