package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String genre,
        @NotNull @PositiveOrZero BigDecimal price) {
}
