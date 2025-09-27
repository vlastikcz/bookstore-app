package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.bookstore.catalog.domain.GenreCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookRequest(
        @NotBlank String title,
        @NotNull List<UUID> authorIds,
        List<GenreCode> genres,
        @NotNull @PositiveOrZero BigDecimal price) {
}
