package com.bank.core.application.service;

import com.bank.core.application.port.in.AccountUseCase;
import com.bank.core.application.port.out.AccountPort;
import com.bank.core.domain.Account;
import com.bank.core.domain.AccountException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService implements AccountUseCase {

    private final AccountPort accountPort;

    public AccountService(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Override
    @Transactional
    public Account createAccount(String customerReference, String currency) {
        Account newAccount = Account.create(customerReference, currency);
        return accountPort.save(newAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountPort.findById(accountId)
                .orElseThrow(() -> new AccountException.AccountNotFoundException(accountId.toString()));
    }
}
