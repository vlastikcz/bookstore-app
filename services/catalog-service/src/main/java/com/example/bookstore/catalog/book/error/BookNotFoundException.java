package com.example.bookstore.catalog.book.error;

import java.util.UUID;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(UUID id) {
        super("Book not found: " + id);
    }
}
