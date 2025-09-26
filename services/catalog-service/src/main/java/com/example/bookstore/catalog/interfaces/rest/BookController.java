package com.example.bookstore.catalog.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.example.bookstore.catalog.application.BookService;
import com.example.bookstore.catalog.application.PreconditionFailedException;
import com.example.bookstore.catalog.application.StrongEtagGenerator;
import com.example.bookstore.catalog.domain.Book;

@RestController
@RequestMapping(value = "/api/books", produces = ApiMediaType.V1_JSON)
@Validated
public class BookController {

    private final BookService service;
    private final StrongEtagGenerator etagGenerator;

    public BookController(BookService service, StrongEtagGenerator etagGenerator) {
        this.service = service;
        this.etagGenerator = etagGenerator;
    }

    @GetMapping(produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<PageResponse<BookResponse>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        Page<Book> result = service.search(title, author, genre, pageable);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(mapToPageResponse(result));
    }

    @GetMapping(value = "/{id}", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<BookResponse> getById(@PathVariable UUID id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        Book book = service.requireById(id);
        String etag = etagGenerator.generate(book.getId(), book.getVersion());

        if (EtagHeaderSupport.matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(etag)
                .body(mapToResponse(book));
    }

    @PutMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> put(@PathVariable UUID id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody @Validated BookRequest request) {

        Book payload = mapToEntity(request);
        payload.setId(id);

        if ("*".equals(ifNoneMatch)) {
            Book created = service.createWithId(id, payload);
            String etag = etagGenerator.generate(created.getId(), created.getVersion());
            return ResponseEntity.created(URI.create("/api/books/" + created.getId()))
                    .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                    .eTag(etag)
                    .body(mapToResponse(created));
        }

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionFailedException("If-Match header is required when updating an existing book");
        }

        Book existing = service.requireById(id);
        String currentEtag = etagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentEtag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        Book updated = service.update(id, payload);
        String etag = etagGenerator.generate(updated.getId(), updated.getVersion());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(etag)
                .body(mapToResponse(updated));
    }

    @PatchMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> patch(@PathVariable UUID id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody @Validated BookPatchRequest request) {
        Book existing = service.requireById(id);
        String currentEtag = etagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentEtag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        if (request.isEmpty()) {
            throw new PreconditionFailedException("Patch request must contain at least one updatable field");
        }

        request.titleValue().ifPresent(existing::setTitle);
        request.authorValue().ifPresent(existing::setAuthor);
        request.genreValue().ifPresent(existing::setGenre);
        request.priceValue().ifPresent(existing::setPrice);

        Book updated = service.save(existing);
        String etag = etagGenerator.generate(updated.getId(), updated.getVersion());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(etag)
                .body(mapToResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @RequestHeader("If-Match") String ifMatch) {
        Book existing = service.requireById(id);
        String currentEtag = etagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentEtag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        service.delete(existing);
        return ResponseEntity.noContent().build();
    }

    private Book mapToEntity(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setGenre(request.genre());
        book.setPrice(request.price());
        return book;
    }

    private BookResponse mapToResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPrice(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    private PageResponse<BookResponse> mapToPageResponse(Page<Book> page) {
        return new PageResponse<>(
                page.map(this::mapToResponse).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
