package com.example.bookstore.catalog.search;

import com.example.bookstore.catalog.search.domain.BookSearchResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record BookSearchItemResponse(
        UUID id,
        String title,
        List<String> authors,
        Double score,
        @JsonProperty("_links") Links links) {

    public static BookSearchItemResponse fromResult(BookSearchResult result) {
        return new BookSearchItemResponse(
                result.id(),
                result.title(),
                result.authors(),
                result.score(),
                new Links(new Link("/api/books/" + result.id()))
        );
    }

    public record Links(Link self) {
    }

    public record Link(String href) {
    }
}
