package com.example.jokesapi.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.jokesapi.model.Joke;
import com.example.jokesapi.service.JokeService;

@RestController
@RequestMapping("/v1/jokes")
public class JokeController {
    private final JokeService service;
    private final com.example.jokesapi.service.IdempotencyService idempotencyService;

    public JokeController(JokeService service, com.example.jokesapi.service.IdempotencyService idempotencyService) { this.service = service; this.idempotencyService = idempotencyService; }

    @GetMapping
    public CollectionModel<EntityModel<Joke>> list() {
        List<EntityModel<Joke>> items = service.findAll().stream()
                .map(j -> EntityModel.of(j,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).get(j.getId())).withSelfRel()))
                .collect(Collectors.toList());
        return CollectionModel.of(items,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).list()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<Joke> get(@PathVariable String id) {
        Joke j = service.findById(id);
        EntityModel<Joke> model = EntityModel.of(j);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).get(id)).withSelfRel());
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).list()).withRel("jokes"));
        return model;
    }

    @PostMapping
    public ResponseEntity<EntityModel<Joke>> create(@RequestHeader HttpHeaders headers, @Validated @RequestBody Joke joke) {
        String idempotency = headers.getFirst("Idempotency-Key");
        Joke created;
        if (idempotency != null) {
            var existing = idempotencyService.lookup(idempotency);
            if (existing.isPresent() && existing.get().startsWith("joke:")) {
                String existingId = existing.get().split(":",2)[1];
                created = service.findById(existingId);
            } else {
                created = service.create(joke);
                idempotencyService.store(idempotency, "joke", created.getId());
            }
        } else {
            throw new IllegalArgumentException("Idempotency-Key header is required for POST requests");
        }
        EntityModel<Joke> model = EntityModel.of(created);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).get(created.getId())).withSelfRel());
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping
    public EntityModel<Joke> upsert(@Validated(value = {Joke.Update.class}) @RequestBody Joke joke) {
        Joke out = service.upsert(joke);
        EntityModel<Joke> model = EntityModel.of(out);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokeController.class).get(out.getId())).withSelfRel());
        return model;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
