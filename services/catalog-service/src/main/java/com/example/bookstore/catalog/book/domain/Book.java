package com.example.bookstore.catalog.book.domain;

import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.common.ResourceMetadata;

import java.util.List;
import java.util.UUID;

public record Book(
        UUID id,
        String title,
        List<UUID> authors,
        List<BookGenre> genres,
        Money price,
        ResourceMetadata metadata) {

    public Book {
        authors = authors == null ? List.of() : List.copyOf(authors);
        genres = genres == null ? List.of() : List.copyOf(genres);
    }
}
