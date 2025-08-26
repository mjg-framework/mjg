package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StationIndicatorEntity implements MigratableEntity {
    private String id = null;

    private String stationCode = null;

    private Integer stationId = null;

    private String indicatorCode = null;

    private Integer indicatorId = null;

    @Override
    public Object getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "StationIndicatorEntity(id=" + id + ", stationCode=" + stationCode + ", stationId=" + stationId + ", indicatorCode=" + indicatorCode + ", indicatorId=" + indicatorId + ")";
    }

    @Override
    public String toString() {
        return getMigratableDescription();
    }
}
