package com.example.mjg.algorithms.cardinality_check;

import com.example.mjg.config.Cardinality;
import com.example.mjg.exceptions.CardinalityViolationException;

public class CardinalityCheck {
    private CardinalityCheck() {}

    public static boolean isConformant(Cardinality cardinality, long numRecords) {
        if (cardinality == Cardinality.EXACTLY_ONE) {
            return numRecords == 1;
        } else if (cardinality == Cardinality.ONE_OR_MORE) {
            return numRecords >= 1;
        } else if (cardinality == Cardinality.ZERO_OR_ONE) {
            return numRecords <= 1;
        }
        return true;
    }

    /**
     * This could be called when the total
     * number of records is yet known (e.g.
     * in a stream iteration progress). If
     * there is any violation spotted, we
     * could stop the operation early.
     * 
     * For example, if the required
     * cardinality is EXACTLY_ONE,
     * then currentNumRecords = 2
     * clearly violates (the number
     * of records may increase only).
     */
    public static boolean isConformantInProgress(Cardinality cardinality, long currentNumRecords) {
        if (cardinality == Cardinality.EXACTLY_ONE || cardinality == Cardinality.ZERO_OR_ONE) {
            return currentNumRecords <= 1;
        }
        return true;
    }

    public static void checkConformant(String migrationFQCN, String annotation, Cardinality cardinality, long numRecords)
    throws CardinalityViolationException {
        if (!isConformant(cardinality, numRecords)) {
            throw new CardinalityViolationException(
                migrationFQCN,
                annotation,
                cardinality,
                numRecords
            );
        }
    }

    public static void checkConformantInProgress(String migrationFQCN, String annotation, Cardinality cardinality, long numRecords)
    throws CardinalityViolationException {
        if (!isConformantInProgress(cardinality, numRecords)) {
            throw new CardinalityViolationException(
                    migrationFQCN,
                    annotation,
                    cardinality,
                    numRecords
            );
        }
    }
}
