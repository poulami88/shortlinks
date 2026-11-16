package com.shortlinks.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) String url,
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$",
                message = "must be 3-32 characters: letters, digits, '-' or '_'") String alias,
        Instant expiresAt) {
}
