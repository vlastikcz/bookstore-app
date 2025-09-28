package com.example.bookstore.catalog.common.error;

import com.example.bookstore.catalog.common.error.RestExceptionHandler.ProblemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void handlePreconditionFailuresReturnsProblem() {
        ResponseEntity<ProblemResponse> response = handler.handlePreconditionFailures(
                new SimpleResponseStatusException(HttpStatus.CONFLICT, "duplicate")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.detail()).contains("duplicate");
    }

    private static class SimpleResponseStatusException extends org.springframework.web.server.ResponseStatusException {
        SimpleResponseStatusException(HttpStatus status, String reason) {
            super(status, reason);
        }
    }
}
