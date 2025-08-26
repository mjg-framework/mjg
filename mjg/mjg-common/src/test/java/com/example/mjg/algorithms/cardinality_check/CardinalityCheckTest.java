package com.example.mjg.algorithms.cardinality_check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.mjg.config.Cardinality;

public class CardinalityCheckTest {
    @Test
    public void testIsConformant_EXACTLY_ONE() {
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.EXACTLY_ONE, 1
            )
        );
        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.EXACTLY_ONE, 0
            )
        );
        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.EXACTLY_ONE, 2
            )
        );
        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.EXACTLY_ONE, 3
            )
        );
    }

    @Test
    public void testIsConformant_ONE_OR_MORE() {
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ONE_OR_MORE, 1
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ONE_OR_MORE, 2
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ONE_OR_MORE, 3
            )
        );

        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.ONE_OR_MORE, 0
            )
        );
    }

    @Test
    public void testIsConformant_ZERO_OR_ONE() {
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_ONE, 0
            )
        );

        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_ONE, 1
            )
        );

        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_ONE, 2
            )
        );

        assertEquals(
            false,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_ONE, 3
            )
        );
    }

    @Test
    public void testIsConformant_ZERO_OR_MORE() {
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_MORE, 0
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_MORE, 1
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_MORE, 2
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformant(
                Cardinality.ZERO_OR_MORE, 3
            )
        );
    }






    @Test
    public void testIsConformantInProgress_EXACTLY_ONE() {
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.EXACTLY_ONE, 1
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.EXACTLY_ONE, 0
            )
        );
        assertEquals(
            false,
            CardinalityCheck.isConformantInProgress(
                Cardinality.EXACTLY_ONE, 2
            )
        );
        assertEquals(
            false,
            CardinalityCheck.isConformantInProgress(
                Cardinality.EXACTLY_ONE, 3
            )
        );
    }

    @Test
    public void testIsConformantInProgress_ONE_OR_MORE() {
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ONE_OR_MORE, 1
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ONE_OR_MORE, 2
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ONE_OR_MORE, 3
            )
        );

        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ONE_OR_MORE, 0
            )
        );
    }

    @Test
    public void testIsConformantInProgress_ZERO_OR_ONE() {
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_ONE, 0
            )
        );

        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_ONE, 1
            )
        );

        assertEquals(
            false,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_ONE, 2
            )
        );

        assertEquals(
            false,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_ONE, 3
            )
        );
    }

    @Test
    public void testIsConformantInProgress_ZERO_OR_MORE() {
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_MORE, 0
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_MORE, 1
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_MORE, 2
            )
        );
        assertEquals(
            true,
            CardinalityCheck.isConformantInProgress(
                Cardinality.ZERO_OR_MORE, 3
            )
        );
    }

}
