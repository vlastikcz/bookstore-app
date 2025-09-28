package com.example.bookstore.catalog.search.domain;

import java.util.List;
import java.util.UUID;

public record BookSearchResult(UUID id, String title, List<String> authors, Double score) {
}
