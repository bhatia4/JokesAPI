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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.jokesapi.model.Jokester;
import com.example.jokesapi.model.PagedResult;
import com.example.jokesapi.service.JokesterService;

@RestController
@RequestMapping("/v1/jokesters")
public class JokesterController {
    private final JokesterService service;
    private final com.example.jokesapi.service.IdempotencyService idempotencyService;

    public JokesterController(JokesterService service, com.example.jokesapi.service.IdempotencyService idempotencyService) { this.service = service; this.idempotencyService = idempotencyService; }

    @GetMapping
    public CollectionModel<EntityModel<Jokester>> list(@RequestParam(required = false) String cursor,
                                                       @RequestParam(defaultValue = "10") int size) {
        PagedResult<Jokester> page = service.findPage(cursor, size);
        List<EntityModel<Jokester>> items = page.items().stream()
                .map(j -> EntityModel.of(j,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(j.getId())).withSelfRel()))
                .collect(Collectors.toList());
        CollectionModel<EntityModel<Jokester>> model = CollectionModel.of(items,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).list(cursor, size)).withSelfRel());
        if (page.nextCursor() != null) {
            model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).list(page.nextCursor(), size)).withRel("next"));
        }
        return model;
    }

    @GetMapping("/{id}")
    public EntityModel<Jokester> get(@PathVariable String id) {
        Jokester j = service.findById(id);
        EntityModel<Jokester> model = EntityModel.of(j);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(id)).withSelfRel());
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).list(null, 10)).withRel("jokesters"));
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
            throw new IllegalArgumentException("Idempotency-Key header is required for POST requests");
        }
        EntityModel<Jokester> model = EntityModel.of(created);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(created.getId())).withSelfRel());
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping
    public EntityModel<Jokester> upsert(@Validated(value = {Jokester.Update.class}) @RequestBody Jokester jokester) {
        Jokester out = service.upsert(jokester);
        EntityModel<Jokester> model = EntityModel.of(out);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(JokesterController.class).get(out.getId())).withSelfRel());
        return model;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
