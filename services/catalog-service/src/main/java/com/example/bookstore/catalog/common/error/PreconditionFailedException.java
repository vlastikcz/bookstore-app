package com.example.bookstore.catalog.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PreconditionFailedException extends ResponseStatusException {

    public PreconditionFailedException(String reason) {
        super(HttpStatus.PRECONDITION_FAILED, reason);
    }
}
