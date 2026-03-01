package com.bank.core.application.port.out;

import com.bank.core.domain.Account;
import java.util.Optional;
import java.util.UUID;

public interface AccountPort {
    Account save(Account account);

    Optional<Account> findById(UUID id);
}
