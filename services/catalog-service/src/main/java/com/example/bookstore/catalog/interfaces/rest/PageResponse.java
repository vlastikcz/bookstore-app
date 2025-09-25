package com.example.bookstore.catalog.interfaces.rest;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
}
