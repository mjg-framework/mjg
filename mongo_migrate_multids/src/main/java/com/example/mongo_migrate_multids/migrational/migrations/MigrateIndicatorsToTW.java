package com.example.mongo_migrate_multids.migrational.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcIndicatorStore;

import java.util.List;
import java.util.Map;

@Migration
@ForEachRecordFrom(
    value = SrcIndicatorStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
@TransformAndSaveTo(
    value = DestIndicatorStore.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
public class MigrateIndicatorsToTW {
    public void startReduction(
        IndicatorEntity indicator,
        Map<String, Object> aggregates
    ) {
    }

    public List<IndicatorEntity> transform(
        Map<String, Object> aggregates,
        IndicatorEntity oldIndicator
    ) {
        IndicatorEntity newIndicator = copyAllExceptId(oldIndicator);

        return List.of(newIndicator);
    }

    public List<IndicatorEntity> handleDuplicate(
        DuplicateDataException exception,
        IndicatorEntity inputRecord,
        List<IndicatorEntity> outputRecords,
        SrcIndicatorStore src,
        DestIndicatorStore dest,
        DataStoreRegistry dataStoreRegistry
    ) {
        // Do not handle duplicate error for now
        return null;
    }



    private static IndicatorEntity copyAllExceptId(
        IndicatorEntity oldIndicator
    ) {
        IndicatorEntity newIndicator = new IndicatorEntity();
        newIndicator.setId(null);

        newIndicator.setExceedValue(oldIndicator.getExceedValue());
        newIndicator.setIndicator(oldIndicator.getIndicator());
        newIndicator.setIndicatorType(oldIndicator.getIndicatorType());
        newIndicator.setPreparingValue(oldIndicator.getPreparingValue());
        newIndicator.setSourceName(oldIndicator.getSourceName());
        newIndicator.setTendencyValue(oldIndicator.getTendencyValue());
        newIndicator.setUnit(oldIndicator.getUnit());

        return newIndicator;
    }
}
