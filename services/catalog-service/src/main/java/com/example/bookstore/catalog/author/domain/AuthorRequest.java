package com.example.bookstore.catalog.author.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest(@NotBlank @Size(max = 255) String name) {
}
