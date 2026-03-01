package com.bank.core.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {
}
