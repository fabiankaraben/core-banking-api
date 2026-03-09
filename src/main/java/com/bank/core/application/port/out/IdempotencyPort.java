package com.bank.core.application.port.out;

import java.util.Optional;

/**
 * Outbound port defining the contract for idempotency key management.
 *
 * <p>The implementation (backed by Redis) ensures that a given transfer request
 * is processed <em>exactly once</em> within a configurable TTL window.
 * The idempotency key (supplied by the caller as the {@code Idempotency-Key} HTTP header)
 * is stored in Redis upon first processing; subsequent requests carrying the same key
 * receive the cached JSON response without re-executing the business logic.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IdempotencyPort {

    /**
     * Attempts to acquire a processing lock for the given idempotency key.
     *
     * <p>This operation is atomic (SET NX EX in Redis). If the key did not exist,
     * it is created with a sentinel "PROCESSING" marker and {@code true} is returned,
     * allowing the caller to proceed. If the key already exists, {@code false} is
     * returned and the caller should retrieve the cached response.</p>
     *
     * @param idempotencyKey the unique key for this request
     * @return {@code true} if the lock was acquired (first-time request),
     *         {@code false} if the key already exists
     */
    boolean tryAcquire(String idempotencyKey);

    /**
     * Stores the serialized JSON response for the given idempotency key.
     *
     * <p>Called after the business logic completes successfully to replace the
     * "PROCESSING" sentinel with the actual response payload. Future calls with
     * the same key will retrieve this stored value.</p>
     *
     * @param idempotencyKey the unique key to associate the response with
     * @param responseJson   the JSON-serialized transaction response
     */
    void store(String idempotencyKey, String responseJson);

    /**
     * Retrieves the previously stored response for the given idempotency key.
     *
     * @param idempotencyKey the key to look up
     * @return an {@link Optional} containing the cached JSON response, or empty if
     *         the key does not exist or has expired
     */
    Optional<String> get(String idempotencyKey);

    /**
     * Removes the idempotency key from the store.
     *
     * <p>Called on error rollback to release an acquired lock so that the caller
     * can safely retry after a transient failure.</p>
     *
     * @param idempotencyKey the key to delete
     */
    void release(String idempotencyKey);
}
