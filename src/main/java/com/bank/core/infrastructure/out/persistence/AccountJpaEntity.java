package com.bank.core.infrastructure.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountJpaEntity {

    @Id
    private UUID id;

    private String customerReference;

    private BigDecimal balance;

    private String currency;

    private String status;

    @Version
    private Long version;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
