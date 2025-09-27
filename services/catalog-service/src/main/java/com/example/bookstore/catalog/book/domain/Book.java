package com.example.bookstore.catalog.book.domain;

import com.example.bookstore.catalog.common.ResourceMetadata;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Book(
        UUID id,
        String title,
        List<UUID> authors,
        List<BookGenre> genres,
        BigDecimal price,
        ResourceMetadata metadata) {
}
