package com.example.bookstore.catalog.book;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookGenre;
import com.example.bookstore.catalog.book.domain.BookPatchRequest;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.ApiMediaType;
import com.example.bookstore.catalog.common.PageResponse;
import com.example.bookstore.catalog.common.PageResponseMeta;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.common.etag.EtagHeaderSupport;
import com.example.bookstore.catalog.common.etag.StrongETagGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/books", produces = ApiMediaType.V1_JSON)
@Validated
public class BookController {

    private final BookService bookService;
    private final StrongETagGenerator eTagGenerator;

    public BookController(@NonNull BookService bookService,
                          @NonNull StrongETagGenerator eTagGenerator) {
        this.bookService = bookService;
        this.eTagGenerator = eTagGenerator;
    }

    @GetMapping(produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<PageResponse<Book>> list(
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize) {

        int resolvedPageNumber = Math.max(pageNumber, 1) - 1;
        int resolvedPageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Pageable pageable = PageRequest.of(
                resolvedPageNumber,
                resolvedPageSize,
                Sort.by("updatedAt").ascending()
        );

        Page<Book> books = bookService.list(pageable);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(mapToPageResponse(books));
    }

    @GetMapping(value = "/{id}", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Book> getById(@PathVariable UUID id,
                                        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Book book = bookService.requireById(id);
        String eTag = eTagGenerator.generate(book.id(), book.metadata().version());

        if (EtagHeaderSupport.matches(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(book);
    }

    @PutMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> put(@PathVariable UUID id,
                                    @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
                                    @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                    @RequestBody @Validated BookRequest request) {

        if ("*".equals(ifNoneMatch)) {
            Book created = bookService.create(id, request);
            String eTag = eTagGenerator.generate(created.id(), created.metadata().version());
            return ResponseEntity.created(URI.create("/api/books/" + created.id()))
                    .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                    .eTag(eTag)
                    .body(created);
        }

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionFailedException("If-Match header is required when updating an existing book");
        }

        Book existing = bookService.requireById(id);
        String currentETag = eTagGenerator.generate(existing.id(), existing.metadata().version());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        Book updated = bookService.update(id, request);
        String eTag = eTagGenerator.generate(updated.id(), updated.metadata().version());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(updated);
    }

    @PatchMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> patch(@PathVariable UUID id,
                                      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
                                      @RequestBody @Validated BookPatchRequest request) {
        Book existing = bookService.requireById(id);
        String currentETag = eTagGenerator.generate(existing.id(), existing.metadata().version());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        if (request.isEmpty()) {
            throw new PreconditionFailedException("Patch request must contain at least one updatable field");
        }

        BookRequest bookRequest = new BookRequest(
                request.titleValue().orElse(existing.title()),
                request.authorsValue().orElse(existing.authors()),
                request.genresValue().map(this::normalizeGenres).orElse(existing.genres()),
                request.priceValue().orElse(existing.price())
        );

        Book updated = bookService.update(id, bookRequest);
        String eTag = eTagGenerator.generate(updated.id(), updated.metadata().version());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
        Book existing = bookService.requireById(id);
        String currentETag = eTagGenerator.generate(existing.id(), existing.metadata().version());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        bookService.delete(existing.id());
        return ResponseEntity.noContent().build();
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

    private PageResponse<Book> mapToPageResponse(Page<Book> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageResponseMeta(
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.getNumber() + 1,
                        page.getSize()
                ));
    }
}
