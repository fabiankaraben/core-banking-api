package com.bank.core.infrastructure.in.web;

import com.bank.core.application.port.in.TransferUseCase;
import com.bank.core.infrastructure.out.idempotency.RedisIdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferUseCase transferUseCase;
    private final RedisIdempotencyService idempotencyService;

    public TransferController(TransferUseCase transferUseCase, RedisIdempotencyService idempotencyService) {
        this.transferUseCase = transferUseCase;
        this.idempotencyService = idempotencyService;
    }

    public record TransferHttpDto(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency) {
    }

    @PostMapping
    public ResponseEntity<Void> executeTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferHttpDto request) {

        if (!idempotencyService.isIdempotencyKeyValid(idempotencyKey)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Conflict indicates this key was already
                                                                       // processed
        }

        TransferUseCase.TransferRequest transferRequest = new TransferUseCase.TransferRequest(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency(),
                idempotencyKey);

        transferUseCase.transfer(transferRequest);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
