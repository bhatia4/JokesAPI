package com.example.jokesapi.service;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class IdempotencyServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void lookupReturnsEmptyForNullOrBlank() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        IdempotencyService svc = new IdempotencyService(redisTemplate);
        assertEquals(Optional.empty(), svc.lookup(null));
        assertEquals(Optional.empty(), svc.lookup(""));
        assertEquals(Optional.empty(), svc.lookup("   "));
    }

    @Test
    @SuppressWarnings("unchecked")
    void lookupReturnsStoredValue() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:abc")).thenReturn("joke:123");

        IdempotencyService svc = new IdempotencyService(redisTemplate);
        Optional<String> out = svc.lookup("abc");
        assertTrue(out.isPresent());
        assertEquals("joke:123", out.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeDoesNothingForBlankKey() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        IdempotencyService svc = new IdempotencyService(redisTemplate);
        svc.store(null, "joke", "1");
        svc.store(" ", "joke", "1");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeSavesValueWithSevenDayTtl() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        IdempotencyService svc = new IdempotencyService(redisTemplate);
        svc.store("k", "joke", "xyz");

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCap.capture(), valCap.capture(), durCap.capture());

        assertEquals("idempotency:k", keyCap.getValue());
        assertEquals("joke:xyz", valCap.getValue());
        assertEquals(Duration.ofDays(7), durCap.getValue());
    }
}
