package com.shortlinks.dto;

import java.time.Instant;

import com.shortlinks.model.Link;

public record LinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount,
        Instant lastAccessedAt) {

    public static LinkResponse from(Link link, String baseUrl) {
        return new LinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount(),
                link.getLastAccessedAt());
    }
}
