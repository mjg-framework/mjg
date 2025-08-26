package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterIndicatorsBy;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.stream.Stream;

public class IndicatorStore extends IntegerIDAbstractStore<IndicatorEntity, FilterIndicatorsBy, Object> {
    @Override
    protected Stream<IndicatorEntity> applyFilter(Stream<IndicatorEntity> recordStream, FilterIndicatorsBy filterType, Object filterValue) {
        if (filterType == FilterIndicatorsBy.ID) {
            return recordStream.filter(record -> record.getId().equals(filterValue));
        }

        if (filterType == FilterIndicatorsBy.INDICATOR_CODE) {
            return recordStream.filter(record -> record.getIndicatorCode().equals(filterValue));
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
}
