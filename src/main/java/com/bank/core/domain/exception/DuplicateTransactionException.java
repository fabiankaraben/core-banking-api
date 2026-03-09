package com.bank.core.domain.exception;

/**
 * Thrown when a transfer request arrives with an {@code Idempotency-Key} that has
 * already been processed within its 24-hour TTL window.
 *
 * <p>This exception is part of the idempotency enforcement mechanism backed by Redis.
 * The REST exception handler maps it to HTTP 409 Conflict, and the cached response
 * from the original request is returned so that the caller can safely retry.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DuplicateTransactionException extends RuntimeException {

    /** The idempotency key that was detected as a duplicate. */
    private final String idempotencyKey;

    /**
     * Constructs a {@code DuplicateTransactionException} for the given idempotency key.
     *
     * @param idempotencyKey the duplicate key received from the caller
     */
    public DuplicateTransactionException(String idempotencyKey) {
        super("Duplicate transaction detected for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * Returns the idempotency key that caused the duplicate detection.
     *
     * @return the idempotency key string
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
