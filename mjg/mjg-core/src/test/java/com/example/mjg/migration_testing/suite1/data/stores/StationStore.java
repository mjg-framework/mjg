package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterStationsBy;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class StationStore extends IntegerIDAbstractStore<StationEntity, FilterStationsBy, Object> {
    @Override
    protected Stream<StationEntity> applyFilter(Stream<StationEntity> recordStream, FilterStationsBy filterType, Object filterValue) {
        if (filterType == FilterStationsBy.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterStationsBy.ID_IN) {
            @SuppressWarnings("unchecked")
            Set<Integer> idIn = (Set<Integer>) filterValue;
            return recordStream.filter(record -> idIn.contains(record.getId()));
        }

        if (filterType == FilterStationsBy.STATION_CODE) {
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

    @Override
    protected Map<FilterStationsBy, Object> doMatchByIdIn(Set<Object> ids) {
        return Map.of(
            FilterStationsBy.ID_IN,
            ids
        );
    }
}
