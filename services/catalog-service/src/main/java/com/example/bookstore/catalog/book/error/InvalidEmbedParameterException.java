package com.example.bookstore.catalog.book.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidEmbedParameterException extends ResponseStatusException {

    public InvalidEmbedParameterException(String value) {
        super(HttpStatus.BAD_REQUEST, "Unsupported embed parameter value: " + value);
    }
}
