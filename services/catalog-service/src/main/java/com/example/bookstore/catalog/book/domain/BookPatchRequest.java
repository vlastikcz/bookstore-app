package com.example.bookstore.catalog.book.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.PositiveOrZero;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookPatchRequest(
        String title,
        List<UUID> authors,
        List<BookGenre> genres,
        @PositiveOrZero BigDecimal price) {

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
    public Optional<BigDecimal> priceValue() {
        return Optional.ofNullable(price);
    }
}
