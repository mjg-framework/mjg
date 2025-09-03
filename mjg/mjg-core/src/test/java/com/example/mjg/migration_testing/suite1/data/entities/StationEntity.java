package com.example.mjg.migration_testing.suite1.data.entities;

import com.example.mjg.data.MigratableEntity;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StationEntity implements MigratableEntity {
    private Integer id = null;

    private String stationCode = null;

    private String stationName = null;

    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "StationEntity(id=" + id + ", stationCode=" + stationCode + ", stationName=" + stationName + ")";
    }
}
