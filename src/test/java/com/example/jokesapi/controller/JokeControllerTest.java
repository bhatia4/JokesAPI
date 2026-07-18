package com.example.jokesapi.controller;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.jokesapi.model.Joke;
import com.example.jokesapi.service.IdempotencyService;
import com.example.jokesapi.service.JokeService;

class JokeControllerTest {

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void createThrowsWhenMissingIdempotencyHeader() {
        JokeService svc = mock(JokeService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        JokeController ctrl = new JokeController(svc, idemp);

        HttpHeaders headers = new HttpHeaders();
        Joke j = new Joke(null, "s", "p", "jid");
        assertThrows(IllegalArgumentException.class, () -> ctrl.create(headers, j));
    }

    @Test
    void createWithExistingIdempotencyReturnsExisting() {
        JokeService svc = mock(JokeService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        when(idemp.lookup("k")).thenReturn(Optional.of("joke:ex"));
        when(svc.findById("ex")).thenReturn(new Joke("ex", "s", "p", "jid"));

        JokeController ctrl = new JokeController(svc, idemp);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", "k");
        ResponseEntity<?> resp = ctrl.create(headers, new Joke(null, "s", "p", "jid"));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Joke body = (Joke) ((org.springframework.hateoas.EntityModel<?>) resp.getBody()).getContent();
        assertEquals("ex", body.getId());
    }

    @Test
    void createWithNewIdempotencyCreatesAndStores() {
        JokeService svc = mock(JokeService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        when(idemp.lookup("k")).thenReturn(Optional.empty());
        when(svc.create(any())).thenAnswer(inv -> {
            Joke in = inv.getArgument(0);
            in.setId("newid");
            return in;
        });

        JokeController ctrl = new JokeController(svc, idemp);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", "k");
        ResponseEntity<?> resp = ctrl.create(headers, new Joke(null, "s", "p", "jid"));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(idemp).store("k", "joke", "newid");
    }
}
