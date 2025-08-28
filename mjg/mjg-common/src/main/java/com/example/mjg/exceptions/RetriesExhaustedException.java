package com.example.mjg.exceptions;

public class RetriesExhaustedException extends BaseMigrationException {
    public RetriesExhaustedException(String message) {
        super(message);
    }

    public RetriesExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
