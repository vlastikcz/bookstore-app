package com.example.bookstore.catalog.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookstore.catalog.application.AuthorService;
import com.example.bookstore.catalog.application.PreconditionFailedException;
import com.example.bookstore.catalog.application.StrongETagGenerator;
import com.example.bookstore.catalog.domain.Author;

@RestController
@RequestMapping(value = "/api/authors", produces = ApiMediaType.V1_JSON)
@Validated
public class AuthorController {

    private final AuthorService authorService;
    private final StrongETagGenerator eTagGenerator;

    public AuthorController(AuthorService authorService, StrongETagGenerator eTagGenerator) {
        this.authorService = authorService;
        this.eTagGenerator = eTagGenerator;
    }

    @GetMapping(produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<PageResponse<AuthorResponse>> list(
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize) {

        int resolvedPageNumber = Math.max(pageNumber, 1) - 1;
        int resolvedPageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Pageable pageable = PageRequest.of(resolvedPageNumber, resolvedPageSize, Sort.by("name").ascending());

        Page<Author> authors = authorService.list(pageable);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(mapToPageResponse(authors));
    }

    @PostMapping(consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorResponse> create(@RequestBody @Validated AuthorRequest request) {
        Author author = authorService.create(request.name());
        String eTag = eTagGenerator.generate(author.getId(), author.getVersion());
        return ResponseEntity.created(URI.create("/api/authors/" + author.getId()))
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(mapToResponse(author));
    }

    @GetMapping(value = "/{id}", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<AuthorResponse> getById(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Author author = authorService.requireById(id);
        String eTag = eTagGenerator.generate(author.getId(), author.getVersion());

        if (EtagHeaderSupport.matches(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(mapToResponse(author));
    }

    @PatchMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorResponse> patch(@PathVariable UUID id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestBody @Validated AuthorPatchRequest request) {
        if (request.isEmpty()) {
            throw new PreconditionFailedException("Patch request must contain at least one updatable field");
        }

        Author author = authorService.requireById(id);
        String currentETag = eTagGenerator.generate(author.getId(), author.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        Author updated = request.nameValue()
                .map(name -> authorService.update(id, name))
                .orElse(author);

        String eTag = eTagGenerator.generate(updated.getId(), updated.getVersion());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(mapToResponse(updated));
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
        Author author = authorService.requireById(id);
        String currentETag = eTagGenerator.generate(author.getId(), author.getVersion());
        if (!EtagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AuthorResponse mapToResponse(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getCreatedAt(), author.getUpdatedAt());
    }

    private PageResponse<AuthorResponse> mapToPageResponse(Page<Author> page) {
        return new PageResponse<>(
                page.map(this::mapToResponse).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
