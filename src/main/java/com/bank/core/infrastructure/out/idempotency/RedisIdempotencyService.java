package com.bank.core.infrastructure.out.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isIdempotencyKeyValid(String key) {
        String redisKey = "idempotency:" + key;
        // setIfAbsent acts as a distributed lock/check
        Boolean isKeyNewlySet = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESS_STARTED",
                Duration.ofHours(24));
        return Boolean.TRUE.equals(isKeyNewlySet);
    }
}
