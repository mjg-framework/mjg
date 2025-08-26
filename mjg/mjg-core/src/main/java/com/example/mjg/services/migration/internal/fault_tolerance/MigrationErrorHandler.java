package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.config.DuplicateResolution;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.BaseMigrationException;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class MigrationErrorHandler {
    private final ArrayList<FailedRecord> failedRecords = new ArrayList<>();

    public void handleException(
        MigratableEntity record,
        Exception e,
        ErrorResolution errorResolution,
        DuplicateResolution duplicateResolution
    ) {
        String cause = ExceptionUtils.getRootCauseMessage(e);
        String effect = errorResolution.strategy().name();

        if (e instanceof BaseMigrationException) {

        } else {
            FailedRecord failedRecord = new FailedRecord(
                record.getMigratableId(),
                record.getMigratableDescription(),
                cause,
                effect,
                "RETRY",
                LocalDateTime.now()
            );
        }
    }

    public void retry()
}
