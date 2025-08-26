package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterMeasurementResultsBy;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.stream.Stream;

public class MeasurementResultStore extends IntegerIDAbstractStore<MeasurementResultEntity, FilterMeasurementResultsBy, Object> {
    @Override
    protected Stream<MeasurementResultEntity> applyFilter(Stream<MeasurementResultEntity> recordStream, FilterMeasurementResultsBy filterType, Object filterValue) {
        if (filterType == FilterMeasurementResultsBy.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterMeasurementResultsBy.STATION_INDICATOR_ID) {
            return recordStream.filter(record -> record.getStationIndicatorCode().equals(filterValue));
        }

        throw new IllegalArgumentException("Filter type not supported");
    }

    @Override
    protected Integer getRecordId(MeasurementResultEntity record) {
        return record.getId();
    }

    @Override
    protected void setRecordId(MeasurementResultEntity record, Integer id) {
        record.setId(id);
    }

    @Override
    protected void assignRecordExceptId(MeasurementResultEntity dest, MeasurementResultEntity src) {
        dest.setValue(src.getValue());
        dest.setStationIndicatorCode(src.getStationIndicatorCode());
    }
}
