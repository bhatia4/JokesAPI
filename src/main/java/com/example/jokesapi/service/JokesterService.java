package com.example.jokesapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.jokesapi.exception.ResourceNotFoundException;
import com.example.jokesapi.model.Jokester;
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

    @SuppressWarnings("null")
    public List<Jokester> findAll() {
        Set<Object> ids = redisTemplate.opsForSet().members(idsKey);
        if (ids == null) return Collections.emptyList();
        return ids.stream()
                .map(Object::toString)
                .map(this::findById)
                .collect(Collectors.toList());
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
