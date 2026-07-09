package com.example.jokesapi.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("null")
    public Optional<String> lookup(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        Object v = redisTemplate.opsForValue().get("idempotency:" + key);
        return Optional.ofNullable(v).map(Object::toString);
    }

    public void store(String key, String resourceType, String id) {
        if (key == null || key.isBlank()) return;
        String value = resourceType + ":" + id;
        redisTemplate.opsForValue().set("idempotency:" + key, value, Duration.ofDays(7));
    }
}
