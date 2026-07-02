package com.example.jokesapi.controller;

import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.service.JokesterService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/jokesters")
public class JokesterController {
    private final JokesterService service;
    private final com.example.jokesapi.service.IdempotencyService idempotencyService;

    public JokesterController(JokesterService service, com.example.jokesapi.service.IdempotencyService idempotencyService) { this.service = service; this.idempotencyService = idempotencyService; }

    @GetMapping
    public CollectionModel<EntityModel<Jokester>> list() {
        List<EntityModel<Jokester>> items = service.findAll().stream()
                .map(j -> EntityModel.of(j,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(j.getId())).withSelfRel()))
                .collect(Collectors.toList());
        return CollectionModel.of(items,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).list()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<Jokester> get(@PathVariable String id) {
        Jokester j = service.findById(id);
        EntityModel<Jokester> model = EntityModel.of(j);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(id)).withSelfRel());
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).list()).withRel("jokesters"));
        return model;
    }

    @PostMapping
    public ResponseEntity<EntityModel<Jokester>> create(@RequestHeader HttpHeaders headers, @Validated @RequestBody Jokester jokester) {
        String idempotency = headers.getFirst("Idempotency-Key");
        Jokester created;
        if (idempotency != null) {
            var existing = idempotencyService.lookup(idempotency);
            if (existing.isPresent() && existing.get().startsWith("jokester:")) {
                String existingId = existing.get().split(":",2)[1];
                created = service.findById(existingId);
            } else {
                created = service.create(jokester);
                idempotencyService.store(idempotency, "jokester", created.getId());
            }
        } else {
            created = service.create(jokester);
        }
        EntityModel<Jokester> model = EntityModel.of(created);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(created.getId())).withSelfRel());
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    public EntityModel<Jokester> upsert(@PathVariable String id, @Validated @RequestBody Jokester jokester) {
        Jokester out = service.upsert(id, jokester);
        EntityModel<Jokester> model = EntityModel.of(out);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(out.getId())).withSelfRel());
        return model;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
