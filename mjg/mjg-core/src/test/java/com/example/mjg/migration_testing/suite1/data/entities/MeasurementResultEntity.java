package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeasurementResultEntity implements MigratableEntity {
    private Integer id = null;

    private String stationIndicatorCode = null;

    private double value = 0.0;

    @Override
    public Object getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "MeasurementResultEntity(id=" + id + ", stationIndicatorCode=" + stationIndicatorCode + ", value=" + value + ")";
    }
}
