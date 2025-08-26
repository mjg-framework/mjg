package com.example.mjg.processors.Migration.processing;

import com.example.mjg.processors.Migration.ComptimeMigration;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.stream.Stream;

public class DataStoreNamesValidation {
    @Getter
    private final ProcessingEnvironment processingEnv;

    @Getter
    private final ComptimeMigration comptimeMigration;

    public DataStoreNamesValidation(ProcessingEnvironment processingEnv, ComptimeMigration comptimeMigration) {
        this.processingEnv = processingEnv;
        this.comptimeMigration = comptimeMigration;
    }

    public boolean validate() {
        return collectAllDataStoreFQCNs().map(fqcn -> {
            String dataStoreClassSimpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            if (!dataStoreClassSimpleName.toLowerCase().contains("store")) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Data store class must contain the word 'store': "
                        + dataStoreClassSimpleName
                        + " (FQCN: " + fqcn + ")"
                );
                return false;
            }
            return true;
        }).reduce(true, Boolean::logicalAnd);
    }

    private Stream<String> collectAllDataStoreFQCNs() {
        return Stream.concat(
            Stream.of(
                comptimeMigration.getSrcDataStoreFQCN(),
                comptimeMigration.getDestDataStoreFQCN()
            ),
            comptimeMigration.getMatchingDataStoreFQCNs().stream()
        );
    }
}
