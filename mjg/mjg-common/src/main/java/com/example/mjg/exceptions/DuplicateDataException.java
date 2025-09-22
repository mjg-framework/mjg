package com.example.mjg.exceptions;

/**
 * Data stores' {@code doSave()}, {@code doSaveAll()}
 * implementations could throw this exception in case
 * of duplicate record(s) being saved, which will be
 * handled by the mjg framework according to the
 * currently applicable {@code @DuplicateResolution}
 * setting.
 */
public class DuplicateDataException extends BaseMigrationException {
    public DuplicateDataException(String message) {
        super(message);
    }

    public DuplicateDataException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public DuplicateDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
