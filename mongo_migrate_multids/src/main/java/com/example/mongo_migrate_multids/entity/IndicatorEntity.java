package com.example.mongo_migrate_multids.entity;

import com.example.mjg.data.MigratableEntity;
import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

@Document(collection = "indicators")
@Getter
@Setter
public class IndicatorEntity implements MigratableEntity {
    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return
            "Indicator(id=" + id + ", largeInteger=" + (
                id == null ? "null" : ObjectIdHelpers.convertObjectIdToLargeInteger(id)
            )
            + ", type=" + indicatorType
            + ", indicator=" + indicator
            + ")";
    }



    @Id
    public String id;

    @Field(value = "exceed_value")
    public Double exceedValue;

    @Field(value = "indicator")
    public String indicator;

    @Field(value = "indicator_type")
    public Integer indicatorType;

    @Field(value = "preparing_value")
    public Double preparingValue;

    @Field(value = "source_name")
    public String sourceName;

    @Field(value = "tendency_value")
    public Double tendencyValue;

    @Field(value = "unit")
    public String unit;

//    @DBRef(lazy = true)
//    public List<StationIndicatorEntity> stationIndicator;
}
