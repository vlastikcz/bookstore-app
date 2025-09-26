package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Positive;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookPatchRequest(
        String title,
        String author,
        String genre,
        @Positive BigDecimal price) {

    @JsonIgnore
    public boolean isEmpty() {
        return titleValue().isEmpty()
                && authorValue().isEmpty()
                && genreValue().isEmpty()
                && price == null;
    }

    @JsonIgnore
    public Optional<String> titleValue() {
        return Optional.ofNullable(title).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public Optional<String> authorValue() {
        return Optional.ofNullable(author).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public Optional<String> genreValue() {
        return Optional.ofNullable(genre).map(String::trim).filter(value -> !value.isEmpty());
    }

    @JsonIgnore
    public Optional<BigDecimal> priceValue() {
        return Optional.ofNullable(price);
    }
}
