package com.example.bookstore.catalog.search.repository;

import java.util.List;
import java.util.UUID;

public record BookSearchRow(UUID id, String title, List<String> authors, Double score) {

    public BookSearchRow {
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
