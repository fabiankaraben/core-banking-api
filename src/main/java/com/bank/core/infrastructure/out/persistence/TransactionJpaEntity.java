package com.bank.core.infrastructure.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionJpaEntity {

    @Id
    private UUID id;

    private UUID sourceAccountId;

    private UUID destinationAccountId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private OffsetDateTime createdAt;

}
