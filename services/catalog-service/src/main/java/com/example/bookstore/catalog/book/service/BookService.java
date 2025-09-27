package com.example.bookstore.catalog.book.service;

import java.util.*;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.domain.BookRequest;
import com.example.bookstore.catalog.book.error.BookNotFoundException;
import com.example.bookstore.catalog.book.repository.BookEntity;
import com.example.bookstore.catalog.book.repository.BookRepository;
import com.example.bookstore.catalog.common.Money;
import com.example.bookstore.catalog.common.error.PreconditionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
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
        if (id != null && bookRepository.existsById(id)) {
            throw new IllegalArgumentException("Book with the provided id already exists");
        }
        BookEntity bookEntity = new BookEntity();
        bookEntity.setId(id == null ? UUID.randomUUID() : id);
        bookEntity.setTitle(bookRequest.title());
        bookEntity.setAuthors(bookRequest.authorIds().stream().distinct().toList());
        bookEntity.setGenres(bookRequest.genres());
        applyPrice(bookEntity, bookRequest.price());
        BookEntity persisted = bookRepository.saveAndFlush(bookEntity);
        log.info("book-service: bookId='{}' created with bookRequest='{}'", persisted.getId(), bookRequest);
        return BookMapper.bookEntityToBook(persisted);
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
    public Book update(@NonNull UUID id, long expectedVersion, @NonNull BookRequest updated) {
        BookEntity existing = requireEntityById(id);
        ensureExpectedVersion(existing, expectedVersion);
        applyUpdates(existing, updated);
        BookEntity persisted = bookRepository.saveAndFlush(existing);
        log.info("book-service: bookId='{}' updated with bookRequest='{}'", id, updated);
        return BookMapper.bookEntityToBook(persisted);
    }

    @Transactional
    public void delete(@NonNull UUID id, long expectedVersion) {
        BookEntity existing = requireEntityById(id);
        ensureExpectedVersion(existing, expectedVersion);
        bookRepository.delete(existing);
        bookRepository.flush();
        log.info("book-service: bookId='{}' deleted at expectedVersion='{}'", id, expectedVersion);
    }

    @Transactional
    public void delete(@NonNull Book book) {
        delete(book.id(), book.metadata().version());
    }

    private void applyUpdates(@NonNull BookEntity existing, @NonNull BookRequest updated) {
        existing.setTitle(updated.title());
        existing.setAuthors(updated.authorIds().stream().distinct().toList());
        existing.setGenres(updated.genres());
        applyPrice(existing, updated.price());
    }

    private void applyPrice(@NonNull BookEntity entity, @NonNull Money price) {
        entity.setPrice(price.amount());
        entity.setPriceCurrency(price.currency());
    }

    private void ensureExpectedVersion(@NonNull BookEntity entity, long expectedVersion) {
        if (entity.getVersion() != expectedVersion) {
            throw new PreconditionFailedException(
                    "Entity version mismatch. Expected %d but was %d".formatted(expectedVersion, entity.getVersion()));
        }
    }

}
