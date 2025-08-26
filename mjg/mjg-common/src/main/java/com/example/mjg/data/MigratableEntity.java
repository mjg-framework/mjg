package com.example.mjg.data;

public interface MigratableEntity {
    Object getMigratableId();

    /**
     *
     * @return a string representation of the instance's
     * content to be included in the migration progress
     * report JSON object.
     */
    String getMigratableDescription();
}
