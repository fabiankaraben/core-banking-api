package com.bank.core.domain;

public class AccountException extends RuntimeException {

    public AccountException(String message) {
        super(message);
    }

    public static class InsufficientFundsException extends AccountException {
        public InsufficientFundsException() {
            super("Insufficient funds");
        }
    }

    public static class AccountNotFoundException extends AccountException {
        public AccountNotFoundException(String id) {
            super("Account not found: " + id);
        }
    }

    public static class AccountBlockedException extends AccountException {
        public AccountBlockedException(String id) {
            super("Account is blocked: " + id);
        }
    }
}
