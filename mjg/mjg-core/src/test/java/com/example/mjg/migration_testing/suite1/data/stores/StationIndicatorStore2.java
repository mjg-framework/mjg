package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterStationIndicatorsBy2;
import com.example.mjg.migration_testing.suite1.data.stores.common.StringIDAbstractStore;

import java.util.stream.Stream;

public class StationIndicatorStore2 extends StringIDAbstractStore<StationIndicatorEntity2, FilterStationIndicatorsBy2, Object> {
    @Override
    protected Stream<StationIndicatorEntity2> applyFilter(Stream<StationIndicatorEntity2> recordStream, FilterStationIndicatorsBy2 filterType, Object filterValue) {
        if (filterType == FilterStationIndicatorsBy2.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterStationIndicatorsBy2.STATION_CODE) {
            return recordStream.filter(record -> record.getStationCode().equals(filterValue));
        }

        if (filterType == FilterStationIndicatorsBy2.INDICATOR_CODE) {
            return recordStream.filter(record -> record.getIndicatorCode().equals(filterValue));
        }

        throw new IllegalArgumentException("Filter type not supported");
    }

    @Override
    protected String getRecordId(StationIndicatorEntity2 record) {
        return record.getId();
    }

    @Override
    protected void setRecordId(StationIndicatorEntity2 record, String id) {
        record.setId(id);
    }

    @Override
    protected void assignRecordExceptId(StationIndicatorEntity2 dest, StationIndicatorEntity2 src) {
        dest.setStationCode(src.getStationCode());
        dest.setStationId(src.getStationId());
        dest.setIndicatorCode(src.getIndicatorCode());
        dest.setIndicatorId(src.getIndicatorId());
        dest.setAverageValue(src.getAverageValue());
    }
}
