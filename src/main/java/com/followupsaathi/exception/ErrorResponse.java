package com.followupsaathi.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Consistent JSON error shape for every API error. */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, List<String>> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }
}
