package com.example.bookstore.catalog.book.service;

import com.example.bookstore.catalog.book.domain.Book;
import com.example.bookstore.catalog.book.repository.BookEntity;
import com.example.bookstore.catalog.common.ResourceMetadata;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public class BookMapper {
    public static @NonNull Book bookEntityToBook(@NonNull BookEntity bookEntity) {
        return new Book(
                bookEntity.getId(),
                bookEntity.getTitle(),
                bookEntity.getAuthors(),
                bookEntity.getGenres(),
                bookEntity.getPrice(),
                new ResourceMetadata(
                        bookEntity.getCreatedAt(), bookEntity.getUpdatedAt(), bookEntity.getVersion()
                )
        );
    }

    public static @NonNull BookEntity bookToBookEntity(@NonNull Book book) {
        BookEntity bookEntity = new BookEntity();
        bookEntity.setId(book.id() == null ? UUID.randomUUID() : book.id());
        bookEntity.setTitle(book.title());
        bookEntity.setAuthors(book.authors());
        bookEntity.setGenres(book.genres());
        bookEntity.setPrice(book.price());
        bookEntity.setCreatedAt(book.metadata().createdAt());
        bookEntity.setUpdatedAt(book.metadata().updatedAt());
        bookEntity.setVersion(book.metadata().version());
        return bookEntity;
    }

    static @NonNull List<Book> bookEntitiesToBooks(List<BookEntity> bookEntities) {
        return bookEntities.stream().map(BookMapper::bookEntityToBook).toList();
    }
}
