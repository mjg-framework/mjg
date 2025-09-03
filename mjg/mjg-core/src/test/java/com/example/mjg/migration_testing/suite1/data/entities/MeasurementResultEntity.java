package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeasurementResultEntity implements MigratableEntity {
    private Integer id = null;

    private String stationIndicatorId = null;

    private double value = 0.0;

    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "MeasurementResultEntity(id=" + id + ", stationIndicatorId=" + stationIndicatorId + ", value=" + value + ")";
    }
}
