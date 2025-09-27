package com.example.bookstore.catalog.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookstore.catalog.domain.Author;
import com.example.bookstore.catalog.domain.AuthorRepository;
import com.example.bookstore.catalog.application.ResourceConflictException;

@Service
public class AuthorService {

    private final AuthorRepository repository;

    public AuthorService(AuthorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Author> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Author requireById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Transactional
    public Author create(@NonNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new PreconditionFailedException("Author name must not be blank");
        }

        if (repository.existsByNameIgnoreCase(trimmed)) {
            throw new ResourceConflictException("Author with the provided name already exists");
        }

        Author author = new Author();
        author.setId(UUID.randomUUID());
        author.setName(trimmed);
        return repository.save(author);
    }

    @Transactional
    public Author update(UUID id, String name) {
        Author author = requireById(id);
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                throw new PreconditionFailedException("Author name must not be blank");
            }
            repository.findByNameIgnoreCase(trimmed)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceConflictException("Author with the provided name already exists");
                    });
            author.setName(trimmed);
        }
        return repository.save(author);
    }

    @Transactional
    public void delete(UUID id) {
        Author author = requireById(id);
        repository.delete(author);
    }

    @Transactional(readOnly = true)
    public List<Author> resolveAuthorsInOrder(List<UUID> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return List.of();
        }

        if (authorIds.stream().anyMatch(Objects::isNull)) {
            throw new PreconditionFailedException("Author ids must not contain null values");
        }

        List<UUID> orderedDistinctIds = authorIds.stream()
                .distinct()
                .toList();

        if (orderedDistinctIds.size() != authorIds.size()) {
            throw new PreconditionFailedException("Duplicate author ids are not allowed");
        }

        List<Author> authors = repository.findAllById(orderedDistinctIds);
        if (authors.size() != orderedDistinctIds.size()) {
            throw new PreconditionFailedException("One or more authors could not be resolved");
        }

        Map<UUID, Author> byId = authors.stream()
                .collect(Collectors.toMap(Author::getId, Function.identity()));

        return authorIds.stream()
                .map(id -> {
                    Author author = byId.get(id);
                    if (author == null) {
                        throw new AuthorNotFoundException(id);
                    }
                    return author;
                })
                .toList();
    }
}
