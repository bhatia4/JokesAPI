package com.example.jokesapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.jokesapi.exception.ResourceNotFoundException;
import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.model.PagedResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JokesterService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper;
    private final String prefix = "v1:jokesters:";
    private final String idsKey = "v1:jokesters:ids";

    public JokesterService(RedisTemplate<String, Object> redisTemplate, ObjectMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    public List<Jokester> findAll() {
        return findPage(null, Integer.MAX_VALUE).items();
    }

    public PagedResult<Jokester> findPage(String cursor, int size) {
        Set<Object> ids = redisTemplate.opsForSet().members(idsKey);
        if (ids == null || ids.isEmpty()) return new PagedResult<>(Collections.emptyList(), null);

        List<String> orderedIds = ids.stream().map(obj -> mapper.convertValue(obj, String.class)).toList();
        int startIndex = 0;
        if (cursor != null && !cursor.isBlank()) {
            int cursorIndex = orderedIds.indexOf(cursor);
            if (cursorIndex >= 0) {
                startIndex = cursorIndex + 1;
            }
        }

        int endIndex = Math.min(startIndex + Math.max(1, size), orderedIds.size());
        List<Jokester> items = orderedIds.subList(startIndex, endIndex).stream()
                .map(this::findById)
                .toList();

        String nextCursor = null;
        if (endIndex < orderedIds.size()) {
            nextCursor = orderedIds.get(endIndex - 1);
        }
        return new PagedResult<>(items, nextCursor);
    }

    public Jokester findById(String id) {
        Object val = redisTemplate.opsForValue().get(prefix + id);
        if (val == null) throw new ResourceNotFoundException("Jokester not found: " + id);
        return mapper.convertValue(val, Jokester.class);
    }

    public Jokester create(Jokester jokester) {
        jokester.setId(UUID.randomUUID().toString()); //always set new Id for create call (ignore Id that was sent)
        redisTemplate.opsForValue().set(prefix + jokester.getId(), jokester);
        redisTemplate.opsForSet().add(idsKey, jokester.getId());
        return jokester;
    }

    public Jokester upsert(Jokester jokester) {
        redisTemplate.opsForValue().set(prefix + jokester.getId(), jokester);
        redisTemplate.opsForSet().add(idsKey, jokester.getId());
        return jokester;
    }

    public void delete(String id) {
        redisTemplate.delete(prefix + id);
        redisTemplate.opsForSet().remove(idsKey, id);
    }
}
