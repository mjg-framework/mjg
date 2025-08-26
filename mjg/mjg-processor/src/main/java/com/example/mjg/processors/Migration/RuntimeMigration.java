package com.example.mjg.processors.Migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RuntimeMigration {
    private String migrationFQCN;

    /**
     *
     * @return the Java source code that
     * is used to create the same object
     */
    public String repr() {
        return "new " + getClass().getCanonicalName() + "(" + reprString(migrationFQCN) + ")";
    }

    private static String reprString(String s) {
        return "\"" + s.replaceAll("\"", "\\\\\"") + "\"";
    }

    @Override
    public String toString() {
        return repr();
    }
}
