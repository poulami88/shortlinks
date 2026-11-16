package com.shortlinks.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shortlinks.config.AppProperties;
import com.shortlinks.dto.CreateLinkRequest;
import com.shortlinks.dto.LinkResponse;
import com.shortlinks.exception.ApiException;
import com.shortlinks.model.Link;
import com.shortlinks.repository.LinkRepository;

@Service
public class LinkService {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_URL_LENGTH = 2048;
    private static final Set<String> RESERVED = Set.of("api", "error", "actuator", "favicon");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final SecureRandom random = new SecureRandom();
    private final LinkRepository repository;
    private final Clock clock;
    private final String baseUrl;

    public LinkService(LinkRepository repository, Clock clock, AppProperties props) {
        this.repository = repository;
        this.clock = clock;
        this.baseUrl = props.baseUrl().replaceAll("/+$", "");
    }

    @Transactional
    public LinkResponse create(CreateLinkRequest request) {
        String url = validateUrl(request.url());
        Instant now = clock.instant();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
        }

        boolean customAlias = request.alias() != null && !request.alias().isBlank();
        String code = customAlias ? validateAlias(request.alias()) : generateCode();

        try {
            Link saved = repository.saveAndFlush(new Link(code, url, now, request.expiresAt()));
            return LinkResponse.from(saved, baseUrl);
        } catch (DataIntegrityViolationException e) {
            // Two requests raced for the same code; the unique constraint decides the winner.
            throw new ApiException(HttpStatus.CONFLICT, "Code '" + code + "' is already taken");
        }
    }

    /** Looks up the target URL for a short code and records the click. */
    @Transactional
    public String resolveAndTrack(String code) {
        Link link = find(code);
        if (link.isExpired(clock.instant())) {
            throw new ApiException(HttpStatus.GONE, "This link has expired");
        }
        repository.recordClick(link.getId(), clock.instant());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public LinkResponse get(String code) {
        return LinkResponse.from(find(code), baseUrl);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> list() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(link -> LinkResponse.from(link, baseUrl))
                .toList();
    }

    @Transactional
    public void delete(String code) {
        repository.delete(find(code));
    }

    private Link find(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw notFound(code);
        }
        return repository.findByCode(code).orElseThrow(() -> notFound(code));
    }

    private ApiException notFound(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "No link found for code '" + code + "'");
    }

    private String validateUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty() || url.length() > MAX_URL_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "URL must be between 1 and " + MAX_URL_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "URL is not valid");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "URL must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "URL must include a host name");
        }
        return url;
    }

    private String validateAlias(String alias) {
        if (!CODE_PATTERN.matcher(alias).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Alias must be 3-32 characters: letters, digits, '-' or '_'");
        }
        if (RESERVED.contains(alias.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Alias '" + alias + "' is reserved");
        }
        if (repository.existsByCode(alias)) {
            throw new ApiException(HttpStatus.CONFLICT, "Code '" + alias + "' is already taken");
        }
        return alias;
    }

    private String generateCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!repository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique code");
    }
}
