package com.bank.core.infrastructure.in.web;

import com.bank.core.application.port.in.AccountUseCase;
import com.bank.core.domain.Account;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    public record CreateAccountRequest(String customerReference, String currency) {
    }

    public record AccountResponse(UUID id, String customerReference, BigDecimal balance, String currency,
            String status) {
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        Account account = accountUseCase.createAccount(request.customerReference(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        Account account = accountUseCase.getAccount(id);
        return ResponseEntity.ok(mapToResponse(account));
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerReference(),
                account.getBalance().amount(),
                account.getBalance().currency(),
                account.getStatus().name());
    }
}
