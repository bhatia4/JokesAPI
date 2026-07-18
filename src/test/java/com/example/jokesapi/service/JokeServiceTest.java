package com.example.jokesapi.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.example.jokesapi.exception.ResourceNotFoundException;
import com.example.jokesapi.model.Joke;
import com.example.jokesapi.model.PagedResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class JokeServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void findPageReturnsEmptyWhenNoIds() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("v1:jokes:ids")).thenReturn(null);

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        PagedResult<Joke> page = service.findPage(null, 10);
        assertTrue(page.items().isEmpty());
        assertNull(page.nextCursor());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPageTreatsUnknownCursorAsStart() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Set<Object> ids = new LinkedHashSet<>(List.of("b", "a", "c"));
        when(setOperations.members("v1:jokes:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokes:a")).thenReturn(new Joke("a", "A", "p", "j1"));
        when(valueOperations.get("v1:jokes:b")).thenReturn(new Joke("b", "B", "p", "j2"));
        when(valueOperations.get("v1:jokes:c")).thenReturn(new Joke("c", "C", "p", "j3"));

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        PagedResult<Joke> page = service.findPage("not-found", 2);
        assertEquals(2, page.items().size());
        assertEquals("b", page.items().get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPageWithSizeZeroReturnsAtLeastOne() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Set<Object> ids = new LinkedHashSet<>(List.of("x", "y"));
        when(setOperations.members("v1:jokes:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokes:x")).thenReturn(new Joke("x", "S", "P", "j1"));
        when(valueOperations.get("v1:jokes:y")).thenReturn(new Joke("y", "S", "P", "j2"));

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        PagedResult<Joke> page = service.findPage(null, 0);
        assertEquals(1, page.items().size());
    }

    @Test
    @SuppressWarnings({"unchecked", "ThrowableResultIgnored"})
    void findByIdThrowsWhenNotFound() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("v1:jokes:missing")).thenReturn(null);

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        assertThrows(ResourceNotFoundException.class, () -> service.findById("missing"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void callUpsertToInsertNewRecord() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        Joke joke = new Joke("id1", "subject1", "prompt1", "joke1");
        Joke result = service.upsert(joke);

        assertEquals(joke.getId(), result.getId());
        assertEquals(joke.getSetup(), result.getSetup());
        assertEquals(joke.getPunchline(), result.getPunchline());
        assertEquals(joke.getJokesterId(), result.getJokesterId());

        verify(valueOperations).set("v1:jokes:id1", joke);
        verify(setOperations).add("v1:jokes:ids", "id1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void callUpsertToUpdateExistingRecord() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());
        Joke existingJoke = new Joke("id2", "subject2", "prompt2", "joke2");
        Joke result = service.upsert(existingJoke);

        assertEquals(result.getId(), existingJoke.getId());
        assertEquals(result.getSetup(), existingJoke.getSetup());
        assertEquals(result.getPunchline(), existingJoke.getPunchline());
        assertEquals(result.getJokesterId(), existingJoke.getJokesterId());
        verify(valueOperations).set("v1:jokes:id2", existingJoke);

        Joke updatedJoke = new Joke("id2", "newSubject", "newPrompt", "newJoke");
        result = service.upsert(updatedJoke);

        assertEquals(result.getId(), updatedJoke.getId());
        assertEquals(result.getSetup(), updatedJoke.getSetup());
        assertEquals(result.getPunchline(), updatedJoke.getPunchline());
        assertEquals(result.getJokesterId(), updatedJoke.getJokesterId());
        verify(valueOperations).set("v1:jokes:id2", updatedJoke);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createUpsertAndDeleteInteractWithRedis() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JokeService service = new JokeService(redisTemplate, new ObjectMapper());

        // create: id is generated
        Joke incoming = new Joke(null, "s", "p", "j1");
        Joke created = service.create(incoming);
        assertNotNull(created.getId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(keyCaptor.capture(), valCaptor.capture());
        String setKey = keyCaptor.getValue();
        assertTrue(setKey.startsWith("v1:jokes:"));
        assertSame(created, valCaptor.getValue());
        verify(setOperations).add("v1:jokes:ids", created.getId());

        // upsert: should use provided id
        Joke up = new Joke("myid", "s2", "p2", "j2");
        Joke out = service.upsert(up);
        assertEquals("myid", out.getId());
        verify(valueOperations).set("v1:jokes:myid", up);
        verify(setOperations).add("v1:jokes:ids", "myid");

        // delete
        service.delete("myid");
        verify(redisTemplate).delete("v1:jokes:myid");
        verify(setOperations).remove("v1:jokes:ids", "myid");
    }
}
