package com.example.bookstore.catalog.book.domain;

import java.util.List;
import java.util.UUID;

import com.example.bookstore.catalog.common.Money;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record BookRequest(
        @NotBlank String title,
        @NotNull List<UUID> authorIds,
        @NotNull List<BookGenre> genres,
        @NotNull @Valid Money price) {
}
