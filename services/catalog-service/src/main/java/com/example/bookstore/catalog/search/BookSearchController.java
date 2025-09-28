package com.example.bookstore.catalog.search;

import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookSort;
import com.example.bookstore.catalog.common.ApiMediaType;
import com.example.bookstore.catalog.common.PageResponse;
import com.example.bookstore.catalog.common.PageResponseMeta;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.search.domain.BookSearchResult;
import com.example.bookstore.catalog.search.service.BookSearchService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/api/book-search", produces = ApiMediaType.V1_JSON)
@Validated
public class BookSearchController {

    private final BookSearchService bookSearchService;

    public BookSearchController(BookSearchService bookSearchService) {
        this.bookSearchService = bookSearchService;
    }

    @GetMapping(produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<PageResponse<BookSearchItemResponse>> searchBooks(
            @RequestParam(name = "filter[title]", required = false) String title,
            @RequestParam(name = "filter[author]", required = false) String author,
            @RequestParam(name = "filter[genres]", required = false) List<BookGenre> genres,
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort", required = false) String sort) {

        int resolvedPageNumber = Math.max(pageNumber, 1) - 1;
        int resolvedPageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);

        Sort resolvedSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(resolvedPageNumber, resolvedPageSize, resolvedSort);
        Page<BookSearchResult> result = bookSearchService.search(title, author, normalizeGenres(genres), pageable);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(toPageResponse(result));
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(BookSort.SCORE, BookSort.UPDATED_AT).descending();
        }

        List<Sort.Order> orders = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(this::mapSortToken)
                .toList();

        return orders.isEmpty()
                ? Sort.by(BookSort.SCORE, BookSort.UPDATED_AT).descending()
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

    private List<BookGenre> normalizeGenres(List<BookGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }

        return genres.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public PageResponse<BookSearchItemResponse> toPageResponse(Page<BookSearchResult> page) {
        return new PageResponse<>(
                page.getContent().stream()
                        .map(BookSearchItemResponse::fromResult)
                        .toList(),
                new PageResponseMeta(
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.getNumber() + 1,
                        page.getSize()
                ));
    }
}
