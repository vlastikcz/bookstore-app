package com.example.bookstore.catalog.author.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.error.AuthorNotFoundException;
import com.example.bookstore.catalog.author.repository.AuthorEntity;
import com.example.bookstore.catalog.author.repository.AuthorRepository;
import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.common.error.ResourceConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.bookstore.catalog.author.service.AuthorMapper.authorEntityToAuthor;

@Service
public class AuthorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);
    private final AuthorRepository repository;
    private final BookService bookService;

    public AuthorService(@NonNull AuthorRepository repository,
                         @NonNull BookService bookService) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.bookService = Objects.requireNonNull(bookService, "bookService must not be null");
    }

    @Transactional(readOnly = true)
    public Page<Author> list(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(AuthorMapper::authorEntityToAuthor);
    }

    @Transactional(readOnly = true)
    public Author requireById(@NonNull UUID id) {
        return authorEntityToAuthor(requireEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<Author> findAllByIds(@NonNull Collection<UUID> ids) {
        Objects.requireNonNull(ids, "ids must not be null");
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream()
                .map(AuthorMapper::authorEntityToAuthor)
                .toList();
    }

    private AuthorEntity requireEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Transactional
    public Author create(UUID id, @NonNull AuthorRequest authorRequest) {
        Objects.requireNonNull(authorRequest, "authorRequest must not be null");
        String trimmed = authorRequest.name() == null ? "" : authorRequest.name().trim();
        if (trimmed.isEmpty()) {
            throw new PreconditionFailedException("Author name must not be blank");
        }

        if (repository.existsByNameIgnoreCase(trimmed)) {
            throw new ResourceConflictException("Author with the provided name already exists");
        }

        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setId(id == null ? UUID.randomUUID() : id);
        authorEntity.setName(trimmed);
        AuthorEntity persisted = repository.saveAndFlush(authorEntity);
        log.info("author-service: authorId='{}' created with authorRequest='{}'", persisted.getId(), authorRequest);
        return authorEntityToAuthor(persisted);
    }

    @Transactional
    public Author update(@NonNull UUID id, long expectedVersion, AuthorRequest authorRequest) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(authorRequest, "authorRequest must not be null");
        AuthorEntity authorEntity = requireEntityById(id);
        ensureExpectedVersion(authorEntity, expectedVersion);
        if (authorRequest.name() != null && !authorRequest.name().isBlank()) {
            String trimmed = authorRequest.name().trim();
            if (trimmed.isEmpty()) {
                throw new PreconditionFailedException("Author name must not be blank");
            }
            repository.findByNameIgnoreCase(trimmed)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceConflictException("Author with the provided name already exists");
                    });
            authorEntity.setName(trimmed);
        }
        AuthorEntity persisted = repository.saveAndFlush(authorEntity);
        log.info("author-service: authorId='{}' updated with authorRequest='{}'", id, authorRequest);
        return authorEntityToAuthor(persisted);
    }

    @Transactional
    public void delete(@NonNull UUID id, long expectedVersion) {
        AuthorEntity authorEntity = requireEntityById(id);
        ensureExpectedVersion(authorEntity, expectedVersion);

        repository.delete(authorEntity);
        repository.flush();
        removeAuthorFromAllBooks(id);
        log.info("author-service: authorId='{}' deleted", id);
    }


    private void removeAuthorFromAllBooks(UUID authorId) {
        List<Book> books = bookService.findByAuthor(authorId);
        books.forEach(b -> {
            BookRequest bookRequest = new BookRequest(
                    b.title(),
                    b.authors().stream().filter(id -> !Objects.equals(id, authorId)).toList(),
                    b.genres(),
                    b.price()
            );
            bookService.update(b.id(), b.metadata().version(), bookRequest);
        });
        if (!books.isEmpty()) {
            log.info("author-service: authorId='{}' removed from '{}' books", authorId, books.size());
        }
    }

    private void ensureExpectedVersion(@NonNull AuthorEntity entity, long expectedVersion) {
        if (entity.getVersion() != expectedVersion) {
            throw new PreconditionFailedException(
                    "Entity version mismatch. Expected %d but was %d".formatted(expectedVersion, entity.getVersion()));
        }
    }
}
