package com.example.bookstore.catalog.book.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.bookstore.catalog.common.Money;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookPatchRequest(
        @Size(max = 255) String title,
        @Size(max = 20) List<@NotNull UUID> authors,
        @Size(max = 20) List<@NotNull BookGenre> genres,
        @Valid Money price) {

    public BookPatchRequest {
        authors = authors == null ? null : List.copyOf(authors);
        genres = genres == null ? null : List.copyOf(genres);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return titleValue().isEmpty()
                && authorsValue().isEmpty()
                && genresValue().isEmpty()
                && price == null;
    }

    @JsonIgnore
    public Optional<String> titleValue() {
        return Optional.ofNullable(title).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public Optional<List<UUID>> authorsValue() {
        return Optional.ofNullable(authors);
    }

    @JsonIgnore
    public Optional<List<BookGenre>> genresValue() {
        return Optional.ofNullable(genres);
    }

    @JsonIgnore
    public Optional<Money> priceValue() {
        return Optional.ofNullable(price);
    }
}
