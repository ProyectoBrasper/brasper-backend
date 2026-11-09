package com.brasper.platform.shared.interfaces.rest.transform;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/** Builds localized RFC 9457 error responses from message keys. */
public final class ProblemDetailAssembler {

    private ProblemDetailAssembler() {
    }

    public static ResponseEntity<ProblemDetail> badRequest(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.BAD_REQUEST, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> unauthorized(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.UNAUTHORIZED, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> forbidden(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.FORBIDDEN, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> notFound(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.NOT_FOUND, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> conflict(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.CONFLICT, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> tooManyRequests(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.TOO_MANY_REQUESTS, messages, key, args);
    }

    public static ResponseEntity<ProblemDetail> unprocessable(MessageSource messages, String key, Object... args) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, messages, key, args);
    }

    private static ResponseEntity<ProblemDetail> of(HttpStatus status, MessageSource messages, String key,
            Object[] args) {
        String detail = messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", key);
        return ResponseEntity.status(status).body(problem);
    }
}
