package com.example.bookstore.catalog.common;

import java.util.List;

public record PageResponse<T>(List<T> content, PageResponseMeta meta) {
}
