package com.example.jokesapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.jokesapi.exception.ResourceNotFoundException;
import com.example.jokesapi.model.Joke;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JokeService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper;
    private final String prefix = "v1:jokes:";
    private final String idsKey = "v1:jokes:ids";

    public JokeService(RedisTemplate<String, Object> redisTemplate, ObjectMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    @SuppressWarnings("null")
    public List<Joke> findAll() {
        Set<Object> ids = redisTemplate.opsForSet().members(idsKey);
        if (ids == null) return Collections.emptyList();
        return ids.stream()
                .map(Object::toString)
                .map(this::findById)
                .collect(Collectors.toList());
    }

    public Joke findById(String id) {
        Object val = redisTemplate.opsForValue().get(prefix + id);
        if (val == null) throw new ResourceNotFoundException("Joke not found: " + id);
        return mapper.convertValue(val, Joke.class);
    }

    public Joke create(Joke joke) {
        if (joke.getId() == null || joke.getId().isBlank()) joke.setId(UUID.randomUUID().toString());
        redisTemplate.opsForValue().set(prefix + joke.getId(), joke);
        redisTemplate.opsForSet().add(idsKey, joke.getId());
        return joke;
    }

    public Joke upsert(String id, Joke joke) {
        if (id == null || id.isBlank()) id = joke.getId();
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        joke.setId(id);
        redisTemplate.opsForValue().set(prefix + id, joke);
        redisTemplate.opsForSet().add(idsKey, id);
        return joke;
    }

    public void delete(String id) {
        redisTemplate.delete(prefix + id);
        redisTemplate.opsForSet().remove(idsKey, id);
    }
}
