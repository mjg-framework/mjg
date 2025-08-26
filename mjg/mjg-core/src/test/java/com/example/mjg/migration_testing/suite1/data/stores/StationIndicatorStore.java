package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterStationIndicatorsBy;
import com.example.mjg.migration_testing.suite1.data.stores.common.StringIDAbstractStore;

import java.util.stream.Stream;

public class StationIndicatorStore extends StringIDAbstractStore<StationIndicatorEntity, FilterStationIndicatorsBy, Object> {
    @Override
    protected Stream<StationIndicatorEntity> applyFilter(Stream<StationIndicatorEntity> recordStream, FilterStationIndicatorsBy filterType, Object filterValue) {
        if (filterType == FilterStationIndicatorsBy.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterStationIndicatorsBy.STATION_CODE) {
            return recordStream.filter(record -> record.getStationCode().equals(filterValue));
        }

        if (filterType == FilterStationIndicatorsBy.INDICATOR_CODE) {
            return recordStream.filter(record -> record.getIndicatorCode().equals(filterValue));
        }

        throw new IllegalArgumentException("Filter type not supported");
    }

    @Override
    protected String getRecordId(StationIndicatorEntity record) {
        return record.getId();
    }

    @Override
    protected void setRecordId(StationIndicatorEntity record, String id) {
        record.setId(id);
    }

    @Override
    protected void assignRecordExceptId(StationIndicatorEntity dest, StationIndicatorEntity src) {
        dest.setStationCode(src.getStationCode());
        dest.setStationId(src.getStationId());
        dest.setIndicatorCode(src.getIndicatorCode());
        dest.setIndicatorId(src.getIndicatorId());
    }
}
