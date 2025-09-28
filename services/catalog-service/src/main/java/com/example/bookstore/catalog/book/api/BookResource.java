package com.example.bookstore.catalog.book.api;

import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.common.ResourceMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookResource(
        UUID id,
        String title,
        List<UUID> authors,
        List<BookGenre> genres,
        Money price,
        ResourceMetadata metadata,
        @JsonProperty("_embedded") BookEmbedded _embedded) {

    public BookResource {
        authors = authors == null ? List.of() : List.copyOf(authors);
        genres = genres == null ? List.of() : List.copyOf(genres);
    }
}
