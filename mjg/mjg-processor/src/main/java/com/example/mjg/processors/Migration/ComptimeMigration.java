package com.example.mjg.processors.Migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.element.TypeElement;
import java.util.List;

@Getter
@AllArgsConstructor
public class ComptimeMigration {
    private TypeElement migrationClass;

    private PMigration pMigration;

    private PForEachRecordFrom pForEachRecordFrom;

    private List<PMatchWith> pMatchWiths;

    private PTransformAndSaveTo pTransformAndSaveTo;

    public String getSrcDataStoreFQCN() {
        return pForEachRecordFrom.getFQCN();
    }

    public List<String> getMatchingDataStoreFQCNs() {
        return pMatchWiths.stream()
                .map(ProcessedAnnotation::getFQCN)
                .toList();
    }

    public String getDestDataStoreFQCN() {
        return pTransformAndSaveTo.getFQCN();
    }
}
