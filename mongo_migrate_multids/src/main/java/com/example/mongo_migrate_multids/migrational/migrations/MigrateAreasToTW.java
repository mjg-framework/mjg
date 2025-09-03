package com.example.mongo_migrate_multids.migrational.migrations;

import java.util.List;
import java.util.Map;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestAreaStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcAreaStore;

@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class,
    batchSize = 512,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
@TransformAndSaveTo(
    value = DestAreaStore.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
public class MigrateAreasToTW {
    public void startReduction(
        AreaEntity inputRecord,
        Map<String, Object> aggregates
    ) {}

    public List<AreaEntity> transform(
        Map<String, Object> aggregates,
        AreaEntity oldArea
    ) {
        AreaEntity newArea = copyAllExceptId(oldArea);
        return List.of(newArea);
    }

    public List<AreaEntity> handleDuplicate(
        DuplicateDataException exception,
        AreaEntity inputRecord,
        List<AreaEntity> outputRecords,
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        // Do not handle duplicate error
        return null;
    }


    private static AreaEntity copyAllExceptId(
        AreaEntity oldArea
    ) {
        AreaEntity newArea = new AreaEntity();
        newArea.setAreaCode(oldArea.getAreaCode());
        newArea.setAreaName(oldArea.getAreaName());
        newArea.setDescription(oldArea.getDescription());
        newArea.setOrderNo(oldArea.getOrderNo());
        return newArea;
    }
}
