package com.shortlinks.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shortlinks.dto.CreateLinkRequest;
import com.shortlinks.dto.LinkResponse;
import com.shortlinks.service.LinkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService service;

    public LinkController(LinkService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/links/" + created.code())).body(created);
    }

    @GetMapping
    public List<LinkResponse> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
