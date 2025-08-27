package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

/**
 * One of "RETRY" | "IGNORE"
 * | "TAKE(0)" | "TAKE(1)" | "TAKE(N-2)" | "TAKE(N-1)"
 * | ... TAKE(x) or TAKE(N - x) with x being a natural number
 * See README.md
 */
public class FailedRecordAction {
    public static enum Type {
        IGNORE,
        RETRY,
        TAKE_ONE
    }

    @Getter
    private String actionString;

    @Getter
    private FailedRecordAction.Type actionType;

    private int relativePositionToTake;
    private boolean fromLast;

    /**
     * 
     * @param N the total number of matched/transformed records
     */
    public int getPositionToTake(int N) {
        if (fromLast) {
            return N - relativePositionToTake;
        } else {
            return relativePositionToTake;
        }
    }

    @JsonCreator
    public FailedRecordAction(String actionString) {
        this.actionString = actionString;
        parse(actionString);
    }

    public FailedRecordAction(FailedRecordAction.Type actionType) {
        if (actionType == FailedRecordAction.Type.TAKE_ONE) {
            throw new RuntimeException("Specify TAKE action without record position to take?");
        }
        this.actionType = actionType;
        this.relativePositionToTake = 0;
        this.fromLast = false;
    }

    public FailedRecordAction(
        FailedRecordAction.Type actionType,
        int relativePositionToTake,
        boolean fromLast
    ) {
        if (actionType != FailedRecordAction.Type.TAKE_ONE) {
            throw new RuntimeException("Specifying position to take when the action is not TAKE?");
        }
        this.actionType = actionType;
        this.relativePositionToTake = relativePositionToTake;
        this.fromLast = fromLast;
    }

    @JsonValue
    public String toValue() {
        return actionString;
    }

    @Override
    public String toString() {
        return actionString;
    }

    private void parse(String actionString) {
        if ("IGNORE".equals(actionString)) {
            this.actionType = FailedRecordAction.Type.IGNORE;
            // N/A
            this.relativePositionToTake = 0;
            this.fromLast = false;
            return;
        }

        if ("RETRY".equals(actionString)) {
            this.actionType = FailedRecordAction.Type.RETRY;
            // N/A
            this.relativePositionToTake = 0;
            this.fromLast = false;
            return;
        }

        {
            Matcher matcher = takeActionAt_X_StringRegex.matcher(actionString);
            if (matcher.matches()) {
                this.actionType = FailedRecordAction.Type.TAKE_ONE;
                this.fromLast = false;

                String numberX = matcher.group(1);
                this.relativePositionToTake = Integer.parseInt(numberX);

                return;
            }
        }

        {
            Matcher matcher = takeActionAt_N_minus_X_StringRegex.matcher(actionString);
            if (matcher.matches()) {
                this.actionType = FailedRecordAction.Type.TAKE_ONE;
                this.fromLast = true;

                String numberX = matcher.group(1);
                this.relativePositionToTake = Integer.parseInt(numberX);

                return;
            }
        }

        throw new RuntimeException("Invalid action string: " + actionString);
    }

    private static final Pattern takeActionAt_X_StringRegex = Pattern.compile(
        "^\\s*TAKE\\s*\\(\\s*(\\d+)\\s*\\)\\s*$"
    );

    private static final Pattern takeActionAt_N_minus_X_StringRegex = Pattern.compile(
        "^\\s*TAKE\\s*\\(\\s*N\\s*\\-\\s*(\\d+)\\s*\\)\\s*$"
    );
}
