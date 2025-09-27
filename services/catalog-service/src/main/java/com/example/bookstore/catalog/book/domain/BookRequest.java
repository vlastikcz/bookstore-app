package com.example.bookstore.catalog.book.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookRequest(
        @NotBlank String title,
        @NotNull List<UUID> authorIds,
        @NotNull List<BookGenre> genres,
        @NotNull @PositiveOrZero BigDecimal price) {
}
