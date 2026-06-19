package com.attus.legalcase.exception;

import com.attus.legalcase.config.CorrelationIdFilter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LegalCaseNotFoundException.class)
    public ProblemDetail handleNotFound(LegalCaseNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Legal case not found", "urn:problem:legal-case-not-found");
    }

    @ExceptionHandler(DuplicateCnjException.class)
    public ProblemDetail handleDuplicate(DuplicateCnjException ex) {
        log.warn("Duplicate CNJ: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage(),
                "Duplicate CNJ", "urn:problem:duplicate-cnj");
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "Invalid status transition", "urn:problem:invalid-status-transition");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "The operation violates a data integrity constraint",
                "Data integrity violation", "urn:problem:data-integrity");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed: {}", errors);

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "One or more fields are invalid",
                "Validation error", "urn:problem:validation-error");
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                "Internal server error", "urn:problem:internal-error");
    }

    private ProblemDetail problem(HttpStatus status, String detail, String title, String type) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(type));
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);
        if (correlationId != null) {
            pd.setProperty("correlationId", correlationId);
        }
        return pd;
    }
}
