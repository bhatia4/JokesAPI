package com.example.jokesapi.startup;

import com.example.jokesapi.model.Joke;
import com.example.jokesapi.model.Jokester;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper;

    public DataLoader(RedisTemplate<String, Object> redisTemplate, ObjectMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            Path jokesPath = Path.of("data/jokes.json");
            Path jokestersPath = Path.of("data/jokesters.json");

            if (Files.exists(jokestersPath)) {
                List<Jokester> jokesters = mapper.readValue(jokestersPath.toFile(), new TypeReference<>() {});
                for (Jokester j : jokesters) {
                    if (j.getId() == null) j.setId(java.util.UUID.randomUUID().toString());
                    redisTemplate.opsForValue().set("v1:jokesters:" + j.getId(), j);
                    redisTemplate.opsForSet().add("v1:jokesters:ids", j.getId());
                }
                log.info("Loaded {} jokesters into Redis", jokesters.size());
            }

            if (Files.exists(jokesPath)) {
                List<Joke> jokes = mapper.readValue(jokesPath.toFile(), new TypeReference<>() {});
                for (Joke k : jokes) {
                    if (k.getId() == null) k.setId(java.util.UUID.randomUUID().toString());
                    redisTemplate.opsForValue().set("v1:jokes:" + k.getId(), k);
                    redisTemplate.opsForSet().add("v1:jokes:ids", k.getId());
                }
                log.info("Loaded {} jokes into Redis", jokes.size());
            }
        } catch (IOException e) {
            log.error("Failed to load initial data", e);
        }
    }
}
