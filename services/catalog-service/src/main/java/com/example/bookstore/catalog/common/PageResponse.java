package com.example.bookstore.catalog.common;

import java.util.List;

public record PageResponse<T>(List<T> content, PageResponseMeta meta) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
