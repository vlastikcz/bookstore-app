package com.example.bookstore.catalog.interfaces.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String genre,
        @NotNull @Positive BigDecimal price) {
}
