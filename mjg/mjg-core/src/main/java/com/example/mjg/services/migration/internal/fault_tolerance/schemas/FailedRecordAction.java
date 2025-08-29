package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import com.example.mjg.exceptions.InvalidActionStringException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

/**
 * One of "RETRY" | "IGNORE"
 * See README.md
 */
public class FailedRecordAction {
    public static enum Type {
        IGNORE,
        RETRY
    }

    @Getter
    private FailedRecordAction.Type actionType;

    public FailedRecordAction(String actionString)
    throws InvalidActionStringException {
        parse(actionString);
    }

    @JsonCreator
    public static FailedRecordAction fromJson(String value)
    throws InvalidActionStringException {
        return new FailedRecordAction(value);
    }

    public FailedRecordAction(FailedRecordAction.Type actionType) {
        if (actionType == null) {
            throw new NullPointerException("FailedRecordAction.Type is null");
        }
        this.actionType = actionType;
    }

    @JsonValue
    public String toValue() {
        return actionType.name();
    }

    @Override
    public String toString() {
        return toValue();
    }

    private void parse(String actionString)
    throws InvalidActionStringException {
        if ("IGNORE".equals(actionString)) {
            this.actionType = FailedRecordAction.Type.IGNORE;
            return;
        }

        if ("RETRY".equals(actionString)) {
            this.actionType = FailedRecordAction.Type.RETRY;
            return;
        }

        throw new InvalidActionStringException(actionString);
    }
}
