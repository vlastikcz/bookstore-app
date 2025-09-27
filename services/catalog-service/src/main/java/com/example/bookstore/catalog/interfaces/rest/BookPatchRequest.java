package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.bookstore.catalog.domain.GenreCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.PositiveOrZero;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookPatchRequest(
        String title,
        List<UUID> authorIds,
        List<GenreCode> genres,
        @PositiveOrZero BigDecimal price) {

    @JsonIgnore
    public boolean isEmpty() {
        return titleValue().isEmpty()
                && authorIdsValue().isEmpty()
                && genresValue().isEmpty()
                && price == null;
    }

    @JsonIgnore
    public Optional<String> titleValue() {
        return Optional.ofNullable(title).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public Optional<List<UUID>> authorIdsValue() {
        return Optional.ofNullable(authorIds);
    }

    @JsonIgnore
    public Optional<List<GenreCode>> genresValue() {
        return Optional.ofNullable(genres);
    }

    @JsonIgnore
    public Optional<BigDecimal> priceValue() {
        return Optional.ofNullable(price);
    }
}
