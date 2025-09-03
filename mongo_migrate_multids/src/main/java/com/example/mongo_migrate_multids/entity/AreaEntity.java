package com.example.mongo_migrate_multids.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.example.mjg.data.MigratableEntity;
import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Document(value = "areas")
public class AreaEntity implements MigratableEntity {
    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "Area(id=" + id + ", largeInteger=" + (
            id == null ? "null" : ObjectIdHelpers.convertObjectIdToLargeInteger(id)
        ) + ")";
    }

    @Id
    public String id;

    @Field(value = "area_code")
    public String areaCode;

    @Field(value = "area_name")
    public String areaName;

    @Field(value = "description")
    public String description;

    @Field(value = "order_no")
    public Object orderNo;
}
