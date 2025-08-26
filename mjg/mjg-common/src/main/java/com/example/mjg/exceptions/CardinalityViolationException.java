package com.example.mjg.exceptions;

import com.example.mjg.config.Cardinality;

public class CardinalityViolationException extends BaseMigrationException {
    public CardinalityViolationException(
        String migrationFQCN,
        String annotation,
        Cardinality cardinality,
        long numRecordsFound
    ) {
        super(
            "Cardinality requirement violated in migration " + migrationFQCN + "\n"
            + "    annotation = " + annotation + "\n"
            + "    required cardinality = " + cardinality + "\n"
            + "    but " + numRecordsFound + " records found.\n"
        );
    }
}
