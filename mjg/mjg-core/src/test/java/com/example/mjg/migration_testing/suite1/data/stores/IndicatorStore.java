package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.IndicatorsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.Set;
import java.util.stream.Stream;

public class IndicatorStore extends IntegerIDAbstractStore<IndicatorEntity, IndicatorsFilterSet> {
    @Override
    protected Stream<IndicatorEntity> applyFilterSet(Stream<IndicatorEntity> recordStream, IndicatorsFilterSet filterSet) {
        if (filterSet.isTakeAll()) return recordStream;

        if (filterSet.getFilterByIdIn() != null) {
            return recordStream.filter(record -> {
                return filterSet.getFilterByIdIn().contains(record.getId());
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
    protected Integer getRecordId(IndicatorEntity record) {
        return record.getId();
    }

    @Override
    protected void setRecordId(IndicatorEntity record, Integer id) {
        record.setId(id);
    }

    @Override
    protected void assignRecordExceptId(IndicatorEntity dest, IndicatorEntity src) {
        dest.setIndicatorCode(src.getIndicatorCode());
        dest.setIndicatorName(src.getIndicatorName());
    }

    @Override
    protected IndicatorsFilterSet doMatchByIdIn(Set<Integer> ids) {
        return IndicatorsFilterSet.filterByIdIn(ids);
    }

    @Override
    protected IndicatorsFilterSet doMatchAll() {
        return IndicatorsFilterSet.takeAll();
    }
}
