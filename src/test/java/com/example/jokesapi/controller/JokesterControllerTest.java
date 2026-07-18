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

import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.service.IdempotencyService;
import com.example.jokesapi.service.JokesterService;

class JokesterControllerTest {

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void createThrowsWhenMissingIdempotencyHeader() {
        JokesterService svc = mock(JokesterService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        JokesterController ctrl = new JokesterController(svc, idemp);

        HttpHeaders headers = new HttpHeaders();
        Jokester j = new Jokester(null, "n", "b");
        assertThrows(IllegalArgumentException.class, () -> ctrl.create(headers, j));
    }

    @Test
    void createWithExistingIdempotencyReturnsExisting() {
        JokesterService svc = mock(JokesterService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        when(idemp.lookup("k")).thenReturn(Optional.of("jokester:ex"));
        when(svc.findById("ex")).thenReturn(new Jokester("ex", "n", "b"));

        JokesterController ctrl = new JokesterController(svc, idemp);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", "k");
        ResponseEntity<?> resp = ctrl.create(headers, new Jokester(null, "n", "b"));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Jokester body = (Jokester) ((org.springframework.hateoas.EntityModel<?>) resp.getBody()).getContent();
        assertEquals("ex", body.getId());
    }

    @Test
    void createWithNewIdempotencyCreatesAndStores() {
        JokesterService svc = mock(JokesterService.class);
        IdempotencyService idemp = mock(IdempotencyService.class);
        when(idemp.lookup("k")).thenReturn(Optional.empty());
        when(svc.create(any())).thenAnswer(inv -> {
            Jokester in = inv.getArgument(0);
            in.setId("newid");
            return in;
        });

        JokesterController ctrl = new JokesterController(svc, idemp);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", "k");
        ResponseEntity<?> resp = ctrl.create(headers, new Jokester(null, "n", "b"));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(idemp).store("k", "jokester", "newid");
    }
}
