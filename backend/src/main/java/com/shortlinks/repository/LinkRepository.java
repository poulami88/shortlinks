package com.shortlinks.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shortlinks.model.Link;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByCode(String code);

    boolean existsByCode(String code);

    List<Link> findTop100ByOrderByCreatedAtDesc();

    /** Atomic increment in the database, so concurrent clicks are never lost. */
    @Modifying(clearAutomatically = true)
    @Query("update Link l set l.clickCount = l.clickCount + 1, l.lastAccessedAt = :now where l.id = :id")
    int recordClick(@Param("id") Long id, @Param("now") Instant now);
}
