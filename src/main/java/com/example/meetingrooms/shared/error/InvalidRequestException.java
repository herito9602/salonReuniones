package com.example.meetingrooms.shared.error;

/**
 * A business-rule validation failure tied to a specific request field (mapped to HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
