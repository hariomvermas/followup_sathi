package com.followupsaathi.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 400 with per-field messages, built from Bean Validation or manual checks. */
public class ValidationException extends ApiException {

    private final Map<String, List<String>> fieldErrors;

    public ValidationException(String message) {
        super(400, message);
        this.fieldErrors = new LinkedHashMap<>();
    }

    public ValidationException(String field, String message) {
        this("Please correct the highlighted fields");
        fieldErrors.put(field, List.of(message));
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }

    public void add(String field, String message) {
        fieldErrors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }

    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
}
