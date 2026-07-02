package com.example.jokesapi.service;

import com.example.jokesapi.exception.ResourceNotFoundException;
import com.example.jokesapi.model.Jokester;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        if (jokester.getId() == null || jokester.getId().isBlank()) jokester.setId(UUID.randomUUID().toString());
        redisTemplate.opsForValue().set(prefix + jokester.getId(), jokester);
        redisTemplate.opsForSet().add(idsKey, jokester.getId());
        return jokester;
    }

    public Jokester upsert(String id, Jokester jokester) {
        if (id == null || id.isBlank()) id = jokester.getId();
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        jokester.setId(id);
        redisTemplate.opsForValue().set(prefix + id, jokester);
        redisTemplate.opsForSet().add(idsKey, id);
        return jokester;
    }

    public void delete(String id) {
        redisTemplate.delete(prefix + id);
        redisTemplate.opsForSet().remove(idsKey, id);
    }
}
