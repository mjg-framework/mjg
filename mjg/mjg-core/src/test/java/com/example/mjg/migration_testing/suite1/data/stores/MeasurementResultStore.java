package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.MeasurementResultsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.Set;
import java.util.stream.Stream;

public class MeasurementResultStore extends IntegerIDAbstractStore<MeasurementResultEntity, MeasurementResultsFilterSet> {
    @Override
    protected Stream<MeasurementResultEntity> applyFilterSet(Stream<MeasurementResultEntity> recordStream, MeasurementResultsFilterSet filterSet) {
        if (filterSet.isTakeAll()) return recordStream;

        if (filterSet.getFilterByIdIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getFilterByIdIn().contains(record.getId());
            });
        }

        if (filterSet.getStationIndicatorIdsIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getStationIndicatorIdsIn().contains(record.getStationIndicatorId());
            });
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
        dest.setStationIndicatorId(src.getStationIndicatorId());
    }

    @Override
    protected MeasurementResultsFilterSet doMatchByIdIn(Set<Integer> ids) {
        return MeasurementResultsFilterSet.filterByIdIn(ids);
    }

    @Override
    protected MeasurementResultsFilterSet doMatchAll() {
        return MeasurementResultsFilterSet.takeAll();
    }
}
