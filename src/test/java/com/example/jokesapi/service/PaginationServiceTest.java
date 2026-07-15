package com.example.jokesapi.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.example.jokesapi.model.Joke;
import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.model.PagedResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class PaginationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void jokeServiceReturnsAPagedSliceAndNextCursor() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Set<Object> ids = new LinkedHashSet<>(List.of("b", "a", "c"));
        when(setOperations.members("v1:jokes:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokes:a")).thenReturn(new Joke("a", "A setup", "A punchline", "j1"));
        when(valueOperations.get("v1:jokes:b")).thenReturn(new Joke("b", "B setup", "B punchline", "j2"));
        when(valueOperations.get("v1:jokes:c")).thenReturn(new Joke("c", "C setup", "C punchline", "j3"));

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());

        PagedResult<Joke> firstPage = service.findPage(null, 2);
        assertEquals(2, firstPage.items().size());
        assertEquals("b", firstPage.items().get(0).getId());
        assertEquals("a", firstPage.items().get(1).getId());
        assertEquals("a", firstPage.nextCursor());

        PagedResult<Joke> secondPage = service.findPage(firstPage.nextCursor(), 2);
        assertEquals(1, secondPage.items().size());
        assertEquals("c", secondPage.items().get(0).getId());
        assertEquals(null, secondPage.nextCursor());
    }

    @SuppressWarnings("unchecked")
    @Test
    void jokesterServiceReturnsAPagedSliceAndNextCursor() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Set<Object> ids = new LinkedHashSet<>(List.of("z", "m", "q"));
        when(setOperations.members("v1:jokesters:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokesters:m")).thenReturn(new Jokester("m", "Mina", "bio"));
        when(valueOperations.get("v1:jokesters:q")).thenReturn(new Jokester("q", "Quinn", "bio"));
        when(valueOperations.get("v1:jokesters:z")).thenReturn(new Jokester("z", "Zed", "bio"));

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());

        PagedResult<Jokester> firstPage = service.findPage(null, 2);
        assertEquals(2, firstPage.items().size());
        assertEquals("z", firstPage.items().get(0).getId());
        assertEquals("m", firstPage.items().get(1).getId());
        assertEquals("m", firstPage.nextCursor());

        PagedResult<Jokester> secondPage = service.findPage(firstPage.nextCursor(), 2);
        assertEquals(1, secondPage.items().size());
        assertEquals("q", secondPage.items().get(0).getId());
        assertEquals(null, secondPage.nextCursor());
    }
}
