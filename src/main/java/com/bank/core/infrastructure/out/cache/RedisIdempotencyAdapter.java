package com.bank.core.infrastructure.out.cache;

import com.bank.core.application.port.out.IdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Outbound cache adapter implementing the {@link IdempotencyPort} using Redis.
 *
 * <p>Uses Redis atomic {@code SET NX EX} semantics (via Spring's
 * {@link StringRedisTemplate#opsForValue()}) to guarantee that an idempotency key
 * is claimed by exactly one concurrent request. The TTL (default: 24 hours) is
 * configurable via the {@code banking.idempotency.ttl-hours} property.</p>
 *
 * <p>Key lifecycle within this adapter:</p>
 * <ol>
 *   <li>{@link #tryAcquire} — sets key to {@code "PROCESSING"} with NX+TTL; returns
 *       {@code true} only when the key did not previously exist.</li>
 *   <li>{@link #store} — overwrites the sentinel with the actual JSON response
 *       (keeps the original TTL by resetting it).</li>
 *   <li>{@link #get} — retrieves the current value (sentinel or response).</li>
 *   <li>{@link #release} — deletes the key on error rollback.</li>
 * </ol>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);

    /** Sentinel value written during initial lock acquisition. */
    private static final String PROCESSING_SENTINEL = "PROCESSING";

    /** Redis key prefix to avoid collisions with other key spaces. */
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    /**
     * Constructs a {@code RedisIdempotencyAdapter}.
     *
     * @param redisTemplate the Spring Data Redis template for string operations
     * @param ttlHours      the number of hours an idempotency key is retained
     *                      (injected from {@code banking.idempotency.ttl-hours})
     */
    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate,
                                   @Value("${banking.idempotency.ttl-hours:24}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Atomically sets the key to {@value #PROCESSING_SENTINEL} only if it does
     * not already exist, using the configured TTL.</p>
     *
     * @param idempotencyKey the unique key for this request
     * @return {@code true} if the key was newly created
     */
    @Override
    public boolean tryAcquire(String idempotencyKey) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(prefixed(idempotencyKey), PROCESSING_SENTINEL, ttl);
        log.debug("Idempotency key acquire: key={}, acquired={}", idempotencyKey, acquired);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overwrites the current key value with the actual JSON response and resets
     * the TTL so the response remains available for the full configured window.</p>
     *
     * @param idempotencyKey the key to update
     * @param responseJson   the serialized transfer response
     */
    @Override
    public void store(String idempotencyKey, String responseJson) {
        redisTemplate.opsForValue().set(prefixed(idempotencyKey), responseJson, ttl);
        log.debug("Idempotency key stored: key={}", idempotencyKey);
    }

    /**
     * {@inheritDoc}
     *
     * @param idempotencyKey the key to look up
     * @return an {@link Optional} containing the cached value, or empty if absent
     */
    @Override
    public Optional<String> get(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(prefixed(idempotencyKey));
        if (value == null || PROCESSING_SENTINEL.equals(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deletes the key so a subsequent retry attempt can re-acquire it.</p>
     *
     * @param idempotencyKey the key to remove
     */
    @Override
    public void release(String idempotencyKey) {
        redisTemplate.delete(prefixed(idempotencyKey));
        log.debug("Idempotency key released: key={}", idempotencyKey);
    }

    /**
     * Returns the Redis key with the namespace prefix applied.
     *
     * @param key the raw idempotency key
     * @return the namespaced key
     */
    private String prefixed(String key) {
        return KEY_PREFIX + key;
    }
}
