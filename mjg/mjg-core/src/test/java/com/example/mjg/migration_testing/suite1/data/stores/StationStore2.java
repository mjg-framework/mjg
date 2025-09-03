package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.StationsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.common.IntegerIDAbstractStore;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Pretty much the same as StationStore.
 *
 * Please read the notes in the definition of
 * StationIndicatorStore2 as well.
 */
public class StationStore2 extends IntegerIDAbstractStore<StationEntity, StationsFilterSet> {
    @Override
    protected Stream<StationEntity> applyFilterSet(Stream<StationEntity> recordStream, StationsFilterSet filterSet) {
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
    protected StationsFilterSet doMatchByIdIn(Set<Integer> ids) {
        return StationsFilterSet.filterByIdIn(ids);
    }

    @Override
    protected StationsFilterSet doMatchAll() {
        return StationsFilterSet.takeAll();
    }
}
