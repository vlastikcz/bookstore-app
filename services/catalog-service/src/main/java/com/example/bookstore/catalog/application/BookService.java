package com.example.bookstore.catalog.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookstore.catalog.domain.Book;
import com.example.bookstore.catalog.domain.BookRepository;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Book create(Book book) {
        return repository.save(book);
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String title, String author, String genre, Pageable pageable) {
        return repository.search(normalizeQuery(title), normalizeQuery(author), normalizeQuery(genre), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Book> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public Book update(UUID id, Book updated) {
        return repository.findById(id)
                .map(existing -> applyUpdates(existing, updated))
                .map(repository::save)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Book applyUpdates(Book existing, Book updated) {
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setGenre(updated.getGenre());
        existing.setPrice(updated.getPrice());
        return existing;
    }

    private String normalizeQuery(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }
}
