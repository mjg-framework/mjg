package com.example.mjg.exceptions;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.example.mjg.config.Cardinality;
import com.example.mjg.data.MigratableEntity;

public class CardinalityViolationException extends BaseMigrationException {
    public static CardinalityViolationException of(
        String migrationFQCN,
        String annotation,
        Cardinality cardinality,
        long numRecords
    ) {
        String msg = "Cardinality requirement violated in migration " + migrationFQCN + "\n"
            + "    annotation = " + annotation + "\n"
            + "    required cardinality = " + cardinality + "\n"
            + "    but " + numRecords + " records found. No more information is provided.\n";
            
        return new CardinalityViolationException(msg);
    }

    public static CardinalityViolationException of(
        String migrationFQCN,
        String annotation,
        Cardinality cardinality,
        long totalRecords,
        List<MigratableEntity> recentRecords
    ) {
        String msg = "Cardinality requirement violated in migration " + migrationFQCN + "\n"
            + "    annotation = " + annotation + "\n"
            + "    required cardinality = " + cardinality + "\n"
            + "    but " + totalRecords + " records found. The most recent ones are:\n";

        try {
            msg += String.join(", ", recentRecords.stream().map(MigratableEntity::getMigratableDescription).toList());
        } catch (Exception e1) {
            try {
                msg += String.join(", ", recentRecords.stream().map(record -> "<ID: " + record.getMigratableId() + ">").toList());
            } catch (Exception e2) {
                msg += "\nERROR: Could not get descriptions or IDs of records! Original exceptions:"
                    + "\n\nEXCEPTION 1 (while getting by descriptions):" + ExceptionUtils.getStackTrace(e1)
                    + "\n\nEXCEPTION 2:" + ExceptionUtils.getStackTrace(e2);
            }
        }

        return new CardinalityViolationException(msg);
    }

    private CardinalityViolationException(String msg) {
        super(msg);
    }
}
