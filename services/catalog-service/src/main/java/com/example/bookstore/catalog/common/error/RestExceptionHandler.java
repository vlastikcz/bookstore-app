package com.example.bookstore.catalog.common.error;

import com.example.bookstore.catalog.author.error.AuthorNotFoundException;
import com.example.bookstore.catalog.book.error.BookNotFoundException;
import com.example.bookstore.catalog.book.error.InvalidEmbedParameterException;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class, AuthorNotFoundException.class})
    public ResponseEntity<ProblemResponse> handleResourceNotFound(RuntimeException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemResponse response = ProblemResponse.of(status, status.getReasonPhrase(), ex.getMessage(), null);
        return problem(status, response);
    }

    @ExceptionHandler({PreconditionFailedException.class, ResourceConflictException.class,
            InvalidEmbedParameterException.class})
    public ResponseEntity<ProblemResponse> handlePreconditionFailures(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ProblemResponse response = ProblemResponse.of(status, status.getReasonPhrase(), ex.getReason(), null);
        return problem(status, response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemResponse> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemResponse response = ProblemResponse.of(status, status.getReasonPhrase(),
                ex.getMessage() == null ? "Access is denied" : ex.getMessage(), null);
        return problem(status, response);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ProblemResponse> handleOptimisticLock(Exception ex) {
        HttpStatus status = HttpStatus.PRECONDITION_FAILED;
        ProblemResponse response = ProblemResponse.of(status, status.getReasonPhrase(),
                "Concurrent modification detected. Please retry with the latest representation.", null);
        return problem(status, response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ProblemResponse.Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ProblemResponse.Violation(fieldError.getField(), getMessage(fieldError)))
                .collect(Collectors.toList());
        ProblemResponse response = ProblemResponse.of(status, "Validation failed",
                "Request payload contains invalid fields.", violations);
        return problem(status, response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemResponse> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ProblemResponse.Violation> violations = ex.getConstraintViolations().stream()
                .map(violation -> new ProblemResponse.Violation(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());
        ProblemResponse response = ProblemResponse.of(status, "Validation failed",
                "Request parameters violate constraints.", violations);
        return problem(status, response);
    }

    private String getMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
    }

    private ResponseEntity<ProblemResponse> problem(HttpStatus status, ProblemResponse response) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProblemResponse(String type,
                                  String title,
                                  int status,
                                  String detail,
                                  String instance,
                                  String code,
                                  List<Violation> violations) {

        public ProblemResponse {
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public static ProblemResponse of(HttpStatus status, String title, String detail,
                                         List<Violation> violations) {
            Objects.requireNonNull(status, "status must not be null");
            return new ProblemResponse(null,
                    title,
                    status.value(),
                    detail,
                    null,
                    null,
                    violations);
        }

        public record Violation(String field, String message) {
        }
    }
}
