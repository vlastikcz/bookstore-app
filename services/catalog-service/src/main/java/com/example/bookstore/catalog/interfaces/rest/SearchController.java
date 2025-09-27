package com.example.bookstore.catalog.interfaces.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookstore.catalog.application.BookService;
import com.example.bookstore.catalog.application.PreconditionFailedException;
import com.example.bookstore.catalog.domain.Book;
import com.example.bookstore.catalog.domain.BookSort;
import com.example.bookstore.catalog.domain.GenreCode;

@RestController
@RequestMapping(value = "/api/search", produces = ApiMediaType.V1_JSON)
@Validated
public class SearchController {

    private final BookService bookService;
    private final BookRepresentationMapper bookRepresentationMapper;

    public SearchController(BookService bookService, BookRepresentationMapper bookRepresentationMapper) {
        this.bookService = bookService;
        this.bookRepresentationMapper = bookRepresentationMapper;
    }

    @GetMapping(value = "/books", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<PageResponse<BookResponse>> searchBooks(
            @RequestParam(name = "filter[title]", required = false) String title,
            @RequestParam(name = "filter[author]", required = false) String author,
            @RequestParam(name = "filter[genres]", required = false) List<GenreCode> genres,
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort", required = false) String sort) {

        int resolvedPageNumber = Math.max(pageNumber, 1) - 1;
        int resolvedPageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);

        Sort resolvedSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(resolvedPageNumber, resolvedPageSize, resolvedSort);
        Page<Book> result = bookService.search(title, author, normalizeGenres(genres), pageable);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(bookRepresentationMapper.toPageResponse(result));
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(BookSort.SCORE, BookSort.CREATED_AT).descending();
        }

        List<Sort.Order> orders = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(this::mapSortToken)
                .toList();

        return orders.isEmpty()
                ? Sort.by(BookSort.SCORE, BookSort.CREATED_AT).descending()
                : Sort.by(orders);
    }

    private Sort.Order mapSortToken(String token) {
        boolean descending = token.startsWith("-");
        String property = descending ? token.substring(1) : token;

        if (property.isBlank()) {
            throw new PreconditionFailedException("Sort token must not be empty");
        }

        if (!isSortableProperty(property)) {
            throw new PreconditionFailedException("Unsupported sort property: " + property);
        }

        return new Sort.Order(descending ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

    private boolean isSortableProperty(String property) {
        return switch (property) {
            case BookSort.TITLE, BookSort.AUTHOR, BookSort.GENRE, BookSort.PRICE,
                    BookSort.CREATED_AT, BookSort.UPDATED_AT, BookSort.SCORE -> true;
            default -> false;
        };
    }

    private List<GenreCode> normalizeGenres(List<GenreCode> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }

        return genres.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
