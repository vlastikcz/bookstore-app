package com.example.bookstore.catalog.author.domain;

import com.example.bookstore.catalog.common.ResourceMetadata;

import java.util.UUID;

public record Author(
        UUID id,
        String name,
        ResourceMetadata metadata) {
}
