package com.brasper.platform.shared.interfaces.rest;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ResponseEntity<ProblemDetail> response = ProblemDetailAssembler.badRequest(messages, "error.validation");
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handle(IllegalArgumentException ex) {
        return ProblemDetailAssembler.badRequest(messages, "error.invalid-argument", ex.getMessage());
    }

    /** E.g. two requests created the same unique value at once; the database constraint is the last guard. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handle(DataIntegrityViolationException ex) {
        return ProblemDetailAssembler.conflict(messages, "error.data-conflict");
    }

    /** Two requests changed the same record at once: the second one lost and must be retried. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handle(OptimisticLockingFailureException ex) {
        return ProblemDetailAssembler.conflict(messages, "error.data-conflict");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handle(IllegalStateException ex) {
        return ProblemDetailAssembler.conflict(messages, "error.invalid-state", ex.getMessage());
    }
}
