package com.example.bookstore.catalog.author;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.domain.AuthorPatchRequest;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.common.ApiMediaType;
import com.example.bookstore.catalog.common.PageResponse;
import com.example.bookstore.catalog.common.PageResponseMeta;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.common.etag.ETagHeaderSupport;
import com.example.bookstore.catalog.common.etag.StrongETagGenerator;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

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
    public ResponseEntity<PageResponse<Author>> list(
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize) {

        int resolvedPageNumber = Math.max(pageNumber, 1) - 1;
        int resolvedPageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Pageable pageable = PageRequest.of(
                resolvedPageNumber,
                resolvedPageSize,
                Sort.by("updatedAt").ascending()
        );

        Page<Author> authors = authorService.list(pageable);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .body(mapToPageResponse(authors));
    }

    @PutMapping(value = "/{id}", consumes = ApiMediaType.V1_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Author> put(@PathVariable UUID id,
                                      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
                                      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                      @RequestBody @Validated AuthorRequest request) {

        if ("*".equals(ifNoneMatch)) {
            Author created = authorService.create(id, request);
            String eTag = eTagGenerator.generate(created.id(), created.metadata().version());
            return ResponseEntity.created(URI.create("/api/authors/" + created.id()))
                    .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                    .eTag(eTag)
                    .body(created);
        }

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionFailedException("If-Match header is required when updating an existing author");
        }

        Author existing = authorService.requireById(id);
        String currentETag = eTagGenerator.generate(existing.id(), existing.metadata().version());
        if (!ETagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        long expectedVersion = requireVersionFromIfMatch(ifMatch, existing.id());
        Author updated = authorService.update(id, expectedVersion, request);
        String eTag = eTagGenerator.generate(updated.id(), updated.metadata().version());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(updated);
    }

    @GetMapping(value = "/{id}", produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Author> getById(@PathVariable UUID id,
                                          @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Author author = authorService.requireById(id);
        String eTag = eTagGenerator.generate(author.id(), author.metadata().version());

        if (ETagHeaderSupport.matches(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(author);
    }

    @PatchMapping(value = "/{id}", consumes = ApiMediaType.MERGE_PATCH_JSON, produces = ApiMediaType.V1_JSON)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Author> patch(@PathVariable UUID id,
                                        @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
                                        @RequestBody @Validated AuthorPatchRequest request) {
        Author existing = authorService.requireById(id);
        String currentETag = eTagGenerator.generate(existing.id(), existing.metadata().version());
        if (!ETagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        if (request.isEmpty()) {
            throw new PreconditionFailedException("Patch request must contain at least one updatable field");
        }

        long expectedVersion = requireVersionFromIfMatch(ifMatch, existing.id());
        AuthorRequest authorRequest = new AuthorRequest(
                request.nameValue().orElse(existing.name())
        );
        Author updated = authorService.update(id, expectedVersion, authorRequest);

        String eTag = eTagGenerator.generate(updated.id(), updated.metadata().version());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(ApiMediaType.V1_JSON))
                .eTag(eTag)
                .body(updated);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
        Author author = authorService.requireById(id);
        String currentETag = eTagGenerator.generate(author.id(), author.metadata().version());
        if (!ETagHeaderSupport.matches(ifMatch, currentETag)) {
            throw new PreconditionFailedException("If-Match header does not match the current entity tag");
        }

        long expectedVersion = requireVersionFromIfMatch(ifMatch, author.id());
        authorService.delete(id, expectedVersion);
        return ResponseEntity.noContent().build();
    }

    private long requireVersionFromIfMatch(String ifMatch, UUID resourceId) {
        Long extracted = ETagHeaderSupport.extractVersion(ifMatch, resourceId);
        if (extracted == null) {
            throw new PreconditionFailedException("If-Match header must include an entity tag with version information");
        }
        return extracted;
    }

    private PageResponse<Author> mapToPageResponse(Page<Author> page) {
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
