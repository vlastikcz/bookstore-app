package com.example.bookstore.catalog.interfaces.rest;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;

import com.example.bookstore.catalog.application.AuthorService;
import com.example.bookstore.catalog.application.BookService;
import com.example.bookstore.catalog.application.PreconditionFailedException;
import com.example.bookstore.catalog.application.StrongETagGenerator;
import com.example.bookstore.catalog.domain.Book;
import com.example.bookstore.catalog.domain.GenreCode;

@RestController
@RequestMapping(value = "/api/books", produces = ApiMediaType.V1_JSON)
@Validated
public class BookController {

    private final BookService service;
    private final StrongETagGenerator eTagGenerator;
    private final AuthorService authorService;
    private final BookRepresentationMapper bookRepresentationMapper;

    public BookController(BookService service,
            StrongETagGenerator eTagGenerator,
            AuthorService authorService,
            BookRepresentationMapper bookRepresentationMapper) {
        this.service = service;
        this.eTagGenerator = eTagGenerator;
        this.authorService = authorService;
        this.bookRepresentationMapper = bookRepresentationMapper;
    }

    @GetMapping(value = "/{id}", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<BookResponse> getById(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Book book = service.requireById(id);
        String eTag = eTagGenerator.generate(book.getId(), book.getVersion());

        if (EtagHeaderSupport.matches(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(bookRepresentationMapper.toResponse(book));
    }

    @PutMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> put(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody @Validated BookRequest request) {

        Book payload = mapToEntity(request);
        payload.setId(id);

        if ("*".equals(ifNoneMatch)) {
            Book created = service.createWithId(id, payload);
            String eTag = eTagGenerator.generate(created.getId(), created.getVersion());
            return ResponseEntity.created(URI.create("/api/books/" + created.getId()))
                    .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                    .eTag(eTag)
                    .body(bookRepresentationMapper.toResponse(created));
        }

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionFailedException("If-Match header is required when updating an existing book");
        }

        Book existing = service.requireById(id);
        String currentETag = eTagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        Book updated = service.update(id, payload);
        String eTag = eTagGenerator.generate(updated.getId(), updated.getVersion());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(bookRepresentationMapper.toResponse(updated));
    }

    @PatchMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> patch(@PathVariable UUID id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestBody @Validated BookPatchRequest request) {
        Book existing = service.requireById(id);
        String currentETag = eTagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        if (request.isEmpty()) {
            throw new PreconditionFailedException("Patch request must contain at least one updatable field");
        }

        request.titleValue().ifPresent(existing::setTitle);
        request.authorIdsValue().ifPresent(ids -> existing.setAuthors(authorService.resolveAuthorsInOrder(ids)));
        request.genresValue().ifPresent(genres -> existing.setGenres(normalizeGenres(genres)));
        request.priceValue().ifPresent(existing::setPrice);

        Book updated = service.save(existing);
        String eTag = eTagGenerator.generate(updated.getId(), updated.getVersion());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(bookRepresentationMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
        Book existing = service.requireById(id);
        String currentETag = eTagGenerator.generate(existing.getId(), existing.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        service.delete(existing);
        return ResponseEntity.noContent().build();
    }

    private Book mapToEntity(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthors(authorService.resolveAuthorsInOrder(request.authorIds()));
        book.setGenres(normalizeGenres(request.genres()));
        book.setPrice(request.price());
        return book;
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
