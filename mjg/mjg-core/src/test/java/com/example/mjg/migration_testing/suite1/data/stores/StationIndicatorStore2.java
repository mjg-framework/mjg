package com.example.mjg.migration_testing.suite1.data.stores;

import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2;
import com.example.mjg.migration_testing.suite1.data.filtering.StationIndicatorsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.common.StringIDAbstractStore;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Pretty much the same as StationIndicatorStore.
 * We are going to migrate data from StationIndicatorStore (that) to StationIndicatorStore2 (this).
 *
 * Note that for data from one store to be migrated to another,
 * no special condition on the stores is required. In fact, you
 * could pretty much migrate data from stores that vastly differ
 * in schemas and structures, yet by specifying proper data matching,
 * reduction and transformation logic, the data would still flow.
 *
 * The difference could be in: different ID types (e.g., Integer vs. String),
 * different entity types (e.g., StationIndicatorEntity2 vs. StationEntity,
 * whatever), different filter set types, different underlying data
 * structures, etc.
 */
public class StationIndicatorStore2 extends StringIDAbstractStore<StationIndicatorEntity2, StationIndicatorsFilterSet> {
    @Override
    protected Stream<StationIndicatorEntity2> applyFilterSet(Stream<StationIndicatorEntity2> recordStream, StationIndicatorsFilterSet filterSet) {
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
