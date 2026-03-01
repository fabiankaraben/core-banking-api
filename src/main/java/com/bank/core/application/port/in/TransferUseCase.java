package com.bank.core.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransferUseCase {

    public record TransferRequest(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency,
            String idempotencyKey) {
    }

    void transfer(TransferRequest request);
}
