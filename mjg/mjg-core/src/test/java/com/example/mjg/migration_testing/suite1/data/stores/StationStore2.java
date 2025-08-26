package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterStationsBy2;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.stream.Stream;

public class StationStore2 extends IntegerIDAbstractStore<StationEntity, FilterStationsBy2, Object> {
    @Override
    protected Stream<StationEntity> applyFilter(Stream<StationEntity> recordStream, FilterStationsBy2 filterType, Object filterValue) {
        if (filterType == FilterStationsBy2.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterStationsBy2.STATION_CODE) {
            return recordStream.filter(record -> record.getStationCode().equals(filterValue));
        }

        throw new IllegalArgumentException("Filter type not supported");
    }

    @Override
    protected Integer getRecordId(StationEntity record) {
        return record.getId();
    }

    @Override
    protected void setRecordId(StationEntity record, Integer id) {
        record.setId(id);
    }

    @Override
    protected void assignRecordExceptId(StationEntity dest, StationEntity src) {
        dest.setStationCode(src.getStationCode());
        dest.setStationName(src.getStationName());
    }
}
