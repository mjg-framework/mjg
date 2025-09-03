package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndicatorEntity implements MigratableEntity {
    private Integer id = null;

    private String indicatorCode = null;

    private String indicatorName = null;

    @Override
    public Serializable getMigratableId() {
        return this.id;
    }

    @Override
    public String getMigratableDescription() {
        return "IndicatorEntity(id=" + id + ", indicatorCode=" + indicatorCode + ", indicatorName=" + indicatorName + ")";
    }
}
