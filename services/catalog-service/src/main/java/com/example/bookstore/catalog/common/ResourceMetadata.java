package com.example.bookstore.catalog.common;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ResourceMetadata(@NotNull Instant createdAt, @NotNull Instant updatedAt, long version) {

}
