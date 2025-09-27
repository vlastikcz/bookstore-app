package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.bookstore.catalog.domain.GenreCode;

public record BookResponse(
        UUID id,
        String title,
        List<UUID> authors,
        List<GenreCode> genres,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt) {
}
