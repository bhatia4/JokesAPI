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
import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.model.PagedResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class JokesterServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void findPageReturnsEmptyWhenNoIds() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("v1:jokesters:ids")).thenReturn(null);

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
        PagedResult<Jokester> page = service.findPage(null, 10);
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
        when(setOperations.members("v1:jokesters:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokesters:a")).thenReturn(new Jokester("a", "A", "b1"));
        when(valueOperations.get("v1:jokesters:b")).thenReturn(new Jokester("b", "B", "b2"));
        when(valueOperations.get("v1:jokesters:c")).thenReturn(new Jokester("c", "C", "b3"));

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
        PagedResult<Jokester> page = service.findPage("not-found", 2);
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
        when(setOperations.members("v1:jokesters:ids")).thenReturn(ids);
        when(valueOperations.get("v1:jokesters:x")).thenReturn(new Jokester("x", "X", "B1"));
        when(valueOperations.get("v1:jokesters:y")).thenReturn(new Jokester("y", "Y", "B2"));

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
        PagedResult<Jokester> page = service.findPage(null, 0);
        assertEquals(1, page.items().size());
    }

    @Test
    @SuppressWarnings({"unchecked", "ThrowableResultIgnored"})
    void findByIdThrowsWhenNotFound() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("v1:jokesters:missing")).thenReturn(null);

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
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

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
        Jokester jokester = new Jokester("id1", "name1", "bio1");
        Jokester result = service.upsert(jokester);

        assertEquals(jokester.getId(), result.getId());
        assertEquals(jokester.getName(), result.getName());
        assertEquals(jokester.getBio(), result.getBio());

        verify(valueOperations).set("v1:jokesters:id1", jokester);
        verify(setOperations).add("v1:jokesters:ids", "id1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void callUpsertToUpdateExistingRecord() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());
        Jokester existingJokester = new Jokester("id2", "name2", "bio2");
        Jokester result = service.upsert(existingJokester);

        //assert that the existing Jokester is stored correctly
        assertEquals(result.getId(), existingJokester.getId());
        assertEquals(result.getName(), existingJokester.getName());
        assertEquals(result.getBio(), existingJokester.getBio());
        verify(valueOperations).set("v1:jokesters:id2", existingJokester);

        Jokester updatedJokester = new Jokester("id2", "newName", "newBio");
        result = service.upsert(updatedJokester);

        //assert that the returned Jokester has the updated values
        assertEquals(result.getId(), updatedJokester.getId());
        assertEquals(result.getName(), updatedJokester.getName());
        assertEquals(result.getBio(), updatedJokester.getBio());
        verify(valueOperations).set("v1:jokesters:id2", updatedJokester);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createUpsertAndDeleteInteractWithRedis() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JokesterService service = new JokesterService(redisTemplate, new ObjectMapper());

        // create: id is generated
        Jokester incoming = new Jokester(null, "n1", "b1");
        Jokester created = service.create(incoming);
        assertNotNull(created.getId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(keyCaptor.capture(), valCaptor.capture());
        String setKey = keyCaptor.getValue();
        assertTrue(setKey.startsWith("v1:jokesters:"));
        assertSame(created, valCaptor.getValue());
        verify(setOperations).add("v1:jokesters:ids", created.getId());

        // upsert: should use provided id
        Jokester up = new Jokester("myid", "n2", "b2");
        Jokester out = service.upsert(up);
        assertEquals("myid", out.getId());
        verify(valueOperations).set("v1:jokesters:myid", up);
        verify(setOperations).add("v1:jokesters:ids", "myid");

        // delete
        service.delete("myid");
        verify(redisTemplate).delete("v1:jokesters:myid");
        verify(setOperations).remove("v1:jokesters:ids", "myid");
    }
}
