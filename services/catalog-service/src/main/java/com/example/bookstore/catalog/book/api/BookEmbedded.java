package com.example.bookstore.catalog.book.api;

import com.example.bookstore.catalog.author.domain.Author;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BookEmbedded(List<Author> authors) {

    public BookEmbedded {
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
