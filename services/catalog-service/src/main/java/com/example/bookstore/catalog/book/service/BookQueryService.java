package com.example.bookstore.catalog.book.service;

import com.example.bookstore.catalog.author.domain.Author;
import com.example.bookstore.catalog.author.service.AuthorService;
import com.example.bookstore.catalog.book.api.BookEmbedOption;
import com.example.bookstore.catalog.book.api.BookEmbedded;
import com.example.bookstore.catalog.book.api.BookResource;
import com.example.bookstore.catalog.book.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookQueryService {

    private final BookService bookService;
    private final AuthorService authorService;

    public BookQueryService(@NonNull BookService bookService,
                            @NonNull AuthorService authorService) {
        this.bookService = Objects.requireNonNull(bookService, "bookService must not be null");
        this.authorService = Objects.requireNonNull(authorService, "authorService must not be null");
    }

    @Transactional(readOnly = true)
    public Page<BookResource> list(@NonNull Pageable pageable, @NonNull EnumSet<BookEmbedOption> embedOptions) {
        Page<Book> books = bookService.list(pageable);
        Map<UUID, Author> authors = resolveAuthors(books.getContent(), embedOptions);
        return books.map(book -> toResource(book, authors, embedOptions));
    }

    @Transactional(readOnly = true)
    public BookResource requireById(@NonNull UUID id, @NonNull EnumSet<BookEmbedOption> embedOptions) {
        Book book = bookService.requireById(id);
        Map<UUID, Author> authors = resolveAuthors(List.of(book), embedOptions);
        return toResource(book, authors, embedOptions);
    }

    private Map<UUID, Author> resolveAuthors(Collection<Book> books, EnumSet<BookEmbedOption> embedOptions) {
        if (!embedOptions.contains(BookEmbedOption.AUTHORS)) {
            return Map.of();
        }

        Set<UUID> authorIds = books.stream()
                .flatMap(book -> book.authors().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        return authorService.findAllByIds(authorIds).stream()
                .collect(Collectors.toMap(Author::id, author -> author));
    }

    private BookResource toResource(Book book, Map<UUID, Author> authorsById, EnumSet<BookEmbedOption> embedOptions) {
        BookEmbedded embedded = null;
        if (embedOptions.contains(BookEmbedOption.AUTHORS) && !book.authors().isEmpty()) {
            List<Author> authors = book.authors().stream()
                    .map(authorsById::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (!authors.isEmpty()) {
                embedded = new BookEmbedded(authors);
            }
        }
        return new BookResource(
                book.id(),
                book.title(),
                book.authors(),
                book.genres(),
                book.price(),
                book.metadata(),
                embedded
        );
    }
}
