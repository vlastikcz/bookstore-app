package com.example.bookstore.catalog.interfaces.rest;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequest(@NotBlank String name) {
}
