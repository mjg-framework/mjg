package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StationIndicatorEntity2 implements MigratableEntity {
    private String id = null;

    private String stationCode = null;

    private Integer stationId = null;

    private String indicatorCode = null;

    private Integer indicatorId = null;

    private double averageValue = 0.0;

    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "StationIndicatorEntity2(id=" + id + ", stationCode=" + stationCode + ", stationId=" + stationId + ", indicatorCode=" + indicatorCode + ", indicatorId=" + indicatorId + ", averageValue=" + averageValue + ")";
    }
}
