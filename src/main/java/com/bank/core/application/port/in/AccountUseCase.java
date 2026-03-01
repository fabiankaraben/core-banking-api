package com.bank.core.application.port.in;

import com.bank.core.domain.Account;
import java.util.UUID;

public interface AccountUseCase {
    Account createAccount(String customerReference, String currency);

    Account getAccount(UUID accountId);
}
