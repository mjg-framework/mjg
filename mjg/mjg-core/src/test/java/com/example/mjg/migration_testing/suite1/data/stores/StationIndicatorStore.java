package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.StationIndicatorsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.common.StringIDAbstractStore;

import java.util.Set;
import java.util.stream.Stream;

public class StationIndicatorStore extends StringIDAbstractStore<StationIndicatorEntity, StationIndicatorsFilterSet> {
    @Override
    protected Stream<StationIndicatorEntity> applyFilterSet(Stream<StationIndicatorEntity> recordStream, StationIndicatorsFilterSet filterSet) {
        if (filterSet.isTakeAll()) return recordStream;

        if (filterSet.getFilterByIdIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getFilterByIdIn().contains(record.getId());
            });
        }

        if (filterSet.getFilterByStationCodeIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getFilterByStationCodeIn().contains(record.getStationCode());
            });
        }

        if (filterSet.getFilterByIndicatorCodeIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getFilterByIndicatorCodeIn().contains(record.getIndicatorCode());
            });
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

    @Override
    protected StationIndicatorsFilterSet doMatchByIdIn(Set<String> ids) {
        return StationIndicatorsFilterSet.filterByIdIn(ids);
    }

    @Override
    protected StationIndicatorsFilterSet doMatchAll() {
        return StationIndicatorsFilterSet.takeAll();
    }
}
