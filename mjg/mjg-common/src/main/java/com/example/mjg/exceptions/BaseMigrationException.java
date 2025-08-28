package com.example.mjg.exceptions;

public class BaseMigrationException extends Exception {
    public BaseMigrationException(String message) {
        super(message);
    }

    public BaseMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
