package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String author,
        String genre,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt) {
}
