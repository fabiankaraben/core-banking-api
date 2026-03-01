package com.bank.core.infrastructure.out.persistence;

import com.bank.core.application.port.out.AccountPort;
import com.bank.core.domain.Account;
import com.bank.core.domain.Money;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepositoryAdapter implements AccountPort {

    private final JpaAccountRepository jpaAccountRepository;

    public AccountRepositoryAdapter(JpaAccountRepository jpaAccountRepository) {
        this.jpaAccountRepository = jpaAccountRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = AccountJpaEntity.builder()
                .id(account.getId())
                .customerReference(account.getCustomerReference())
                .balance(account.getBalance().amount())
                .currency(account.getBalance().currency())
                .status(account.getStatus().name())
                .version(account.getVersion())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();

        AccountJpaEntity savedEntity = jpaAccountRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaAccountRepository.findById(id).map(this::mapToDomain);
    }

    private Account mapToDomain(AccountJpaEntity entity) {
        return new Account(
                entity.getId(),
                entity.getCustomerReference(),
                Money.of(entity.getBalance(), entity.getCurrency()),
                Account.AccountStatus.valueOf(entity.getStatus()),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
