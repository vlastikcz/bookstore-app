package com.example.bookstore.catalog.book.service;

import java.util.*;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.error.BookNotFoundException;
import com.example.bookstore.catalog.book.repository.BookEntity;
import com.example.bookstore.catalog.book.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(@NonNull BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public Page<Book> list(@NonNull Pageable pageable) {
        return bookRepository.findAll(pageable).map(BookMapper::bookEntityToBook);
    }

    @Transactional(readOnly = true)
    public List<Book> findByAuthor(@NonNull UUID authorId) {
        return bookRepository.findByAuthorId(authorId).stream()
                .map(BookMapper::bookEntityToBook)
                .toList();
    }

    @Transactional
    public Book create(@Nullable UUID id, @NonNull BookRequest bookRequest) {
        Objects.requireNonNull(bookRequest, "bookRequest must not be null");
        BookEntity bookEntity = new BookEntity();
        bookEntity.setId(id == null ? UUID.randomUUID() : id);
        bookEntity.setTitle(bookRequest.title());
        bookEntity.setAuthors(bookRequest.authorIds().stream().distinct().toList());
        bookEntity.setGenres(bookRequest.genres());
        bookEntity.setPrice(bookRequest.price());
        return BookMapper.bookEntityToBook(bookRepository.save(bookEntity));
    }

    @Transactional(readOnly = true)
    public Book requireById(@NonNull UUID id) {
        return BookMapper.bookEntityToBook(requireEntityById(id));
    }

    private BookEntity requireEntityById(@NonNull UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public boolean exists(@NonNull UUID id) {
        return bookRepository.existsById(id);
    }

    @Transactional
    public Book update(@NonNull UUID id, @NonNull BookRequest updated) {
        BookEntity existing = requireEntityById(id);
        applyUpdates(existing, updated);
        return BookMapper.bookEntityToBook(bookRepository.save(existing));
    }

    @Transactional
    public void delete(@NonNull UUID id) {
        BookEntity existing = requireEntityById(id);
        bookRepository.delete(existing);
    }

    @Transactional
    public void delete(@NonNull Book book) {
        BookEntity bookEntity = requireEntityById(book.id());
        bookRepository.delete(bookEntity);
    }

    private void applyUpdates(@NonNull BookEntity existing, @NonNull BookRequest updated) {
        existing.setTitle(updated.title());
        existing.setAuthors(updated.authorIds());
        existing.setAuthors(updated.authorIds().stream().distinct().toList());
        existing.setGenres(updated.genres());
        existing.setPrice(updated.price());
    }

}
