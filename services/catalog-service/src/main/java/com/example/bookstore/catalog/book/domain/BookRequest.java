package com.example.bookstore.catalog.book.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.example.bookstore.catalog.common.Money;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Size(max = 20) List<@NotNull UUID> authorIds,
        @NotNull @Size(max = 20) List<@NotNull BookGenre> genres,
        @NotNull @Valid Money price) {

    public BookRequest {
        authorIds = List.copyOf(Objects.requireNonNull(authorIds, "authorIds must not be null"));
        genres = List.copyOf(Objects.requireNonNull(genres, "genres must not be null"));
    }
}
