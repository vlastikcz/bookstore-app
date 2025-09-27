package com.example.bookstore.catalog.common.error;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.example.bookstore.catalog.author.error.AuthorNotFoundException;
import com.example.bookstore.catalog.book.error.BookNotFoundException;

@RestControllerAdvice
public class RestExceptionHandler {
    private final Clock clock;

    public RestExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({BookNotFoundException.class, AuthorNotFoundException.class})
    public ResponseEntity<SimpleErrorResponse> handleResourceNotFound(RuntimeException ex) {
        SimpleErrorResponse response = new SimpleErrorResponse(
                Instant.now(clock),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({PreconditionFailedException.class, ResourceConflictException.class})
    public ResponseEntity<SimpleErrorResponse> handlePreconditionFailures(ResponseStatusException ex) {
        SimpleErrorResponse response = new SimpleErrorResponse(
                Instant.now(clock),
                ex.getStatusCode().value(),
                ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<SimpleErrorResponse> handleOptimisticLock(Exception ex) {
        SimpleErrorResponse response = new SimpleErrorResponse(
                Instant.now(clock),
                HttpStatus.PRECONDITION_FAILED.value(),
                "Concurrent modification detected. Please retry with the latest representation.");
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldValidationError(fieldError.getField(), getMessage(fieldError)))
                .collect(Collectors.toList());
        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now(clock),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());
        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now(clock),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors);
        return ResponseEntity.badRequest().body(response);
    }

    private String getMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
    }

    public record SimpleErrorResponse(Instant timestamp, int status, String message) {
    }

    public record ValidationErrorResponse(Instant timestamp, int status, String message,
                                          List<FieldValidationError> errors) {
    }

    public record FieldValidationError(String field, String message) {
    }
}
