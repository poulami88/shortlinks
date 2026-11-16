package com.shortlinks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.shortlinks.config.AppProperties;
import com.shortlinks.dto.CreateLinkRequest;
import com.shortlinks.dto.LinkResponse;
import com.shortlinks.exception.ApiException;
import com.shortlinks.model.Link;
import com.shortlinks.repository.LinkRepository;

class LinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    private final LinkRepository repository = mock(LinkRepository.class);
    private final LinkService service = new LinkService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new AppProperties("http://localhost:8080/", "http://localhost:5173"));

    private ApiException failure(CreateLinkRequest request) {
        return assertThrows(ApiException.class, () -> service.create(request));
    }

    @Test
    void generatesSevenCharacterAlphanumericCode() {
        when(repository.saveAndFlush(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkResponse response = service.create(new CreateLinkRequest("https://example.com/a", null, null));

        assertEquals(7, response.code().length());
        assertTrue(response.code().matches("[A-Za-z0-9]{7}"));
        assertEquals("http://localhost:8080/" + response.code(), response.shortUrl());
    }

    @Test
    void usesCustomAliasWhenFree() {
        when(repository.saveAndFlush(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkResponse response = service.create(new CreateLinkRequest("https://example.com", "my-link", null));

        assertEquals("my-link", response.code());
    }

    @Test
    void rejectsTakenAlias() {
        when(repository.existsByCode("taken")).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT,
                failure(new CreateLinkRequest("https://example.com", "taken", null)).getStatus());
    }

    @Test
    void rejectsReservedAlias() {
        assertEquals(HttpStatus.BAD_REQUEST,
                failure(new CreateLinkRequest("https://example.com", "api", null)).getStatus());
    }

    @Test
    void rejectsNonHttpUrls() {
        assertEquals(HttpStatus.BAD_REQUEST,
                failure(new CreateLinkRequest("ftp://example.com/file", null, null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                failure(new CreateLinkRequest("not a url", null, null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                failure(new CreateLinkRequest("http://", null, null)).getStatus());
    }

    @Test
    void rejectsExpiryInThePast() {
        assertEquals(HttpStatus.BAD_REQUEST,
                failure(new CreateLinkRequest("https://example.com", null, NOW.minusSeconds(60))).getStatus());
    }

    @Test
    void expiredLinkReturnsGoneAndIsNotCounted() {
        Link expired = new Link("abc123", "https://example.com", NOW.minusSeconds(7200), NOW.minusSeconds(3600));
        when(repository.findByCode("abc123")).thenReturn(Optional.of(expired));

        ApiException e = assertThrows(ApiException.class, () -> service.resolveAndTrack("abc123"));

        assertEquals(HttpStatus.GONE, e.getStatus());
        verify(repository, never()).recordClick(any(), any());
    }

    @Test
    void resolveRecordsClickAndReturnsTarget() {
        Link link = new Link("abc123", "https://example.com/x", NOW.minusSeconds(60), null);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(link));

        String target = service.resolveAndTrack("abc123");

        assertEquals("https://example.com/x", target);
        verify(repository).recordClick(any(), eq(NOW));
    }

    @Test
    void unknownCodeIsNotFound() {
        when(repository.findByCode("missing")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> service.resolveAndTrack("missing"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
