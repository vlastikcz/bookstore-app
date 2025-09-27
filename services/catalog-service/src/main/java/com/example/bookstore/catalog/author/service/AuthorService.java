package com.example.bookstore.catalog.author.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.domain.AuthorRequest;
import com.example.bookstore.catalog.author.error.AuthorNotFoundException;
import com.example.bookstore.catalog.author.repository.AuthorEntity;
import com.example.bookstore.catalog.author.repository.AuthorRepository;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.service.BookService;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import com.example.bookstore.catalog.common.error.ResourceConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static com.example.bookstore.catalog.author.service.AuthorMapper.authorEntityToAuthor;

@Service
public class AuthorService {

    private final AuthorRepository repository;
    private final BookService bookService;

    public AuthorService(@NonNull AuthorRepository repository,
                         @NonNull BookService bookService) {
        this.repository = repository;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public Page<Author> list(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(AuthorMapper::authorEntityToAuthor);
    }

    @Transactional(readOnly = true)
    public Author requireById(@NonNull UUID id) {
        return authorEntityToAuthor(requireEntityById(id));
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
        return authorEntityToAuthor(repository.save(authorEntity));
    }

    @Transactional
    public Author update(@NonNull UUID id, AuthorRequest authorRequest) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(authorRequest, "authorRequest must not be null");
        AuthorEntity authorEntity = requireEntityById(id);
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
        return authorEntityToAuthor(repository.save(authorEntity));
    }

    @Transactional
    public void delete(@NonNull UUID id) {
        AuthorEntity authorEntity = requireEntityById(id);

        repository.delete(authorEntity);
        removeAuthorFromAllBooks(id);
    }


    private void removeAuthorFromAllBooks(UUID authorId) {
        bookService.findByAuthor(authorId).forEach(b -> {
            BookRequest bookRequest = new BookRequest(
                    b.title(),
                    b.authors().stream().filter(id -> !Objects.equals(id, authorId)).toList(),
                    b.genres(),
                    b.price()
            );
            bookService.update(b.id(), bookRequest);
        });
    }
}
