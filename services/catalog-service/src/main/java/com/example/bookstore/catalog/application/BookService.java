package com.example.bookstore.catalog.application;

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
        if (book.getId() == null) {
            book.setId(UUID.randomUUID());
        }
        return repository.save(book);
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String title, String author, String genre, Pageable pageable) {
        return repository.search(normalizeQuery(title), normalizeQuery(author), normalizeQuery(genre), pageable);
    }

    @Transactional(readOnly = true)
    public Book requireById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return repository.existsById(id);
    }

    @Transactional
    public Book update(UUID id, Book updated) {
        Book existing = requireById(id);
        applyUpdates(existing, updated);
        return repository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Book existing = requireById(id);
        repository.delete(existing);
    }

    @Transactional
    public void delete(Book book) {
        repository.delete(book);
    }

    @Transactional
    public Book createWithId(UUID id, Book book) {
        if (repository.existsById(id)) {
            throw new ResourceConflictException("Book with the provided identifier already exists");
        }

        book.setId(id);
        return repository.save(book);
    }

    @Transactional
    public Book save(Book book) {
        return repository.save(book);
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
