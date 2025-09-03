package com.example.mjg.data;

import java.io.Serializable;

public interface MigratableEntity {
    Serializable getMigratableId();

    /**
     *
     * @return a string representation of the instance's
     * content to be included in the migration progress
     * report JSON object.
     */
    String getMigratableDescription();
}
