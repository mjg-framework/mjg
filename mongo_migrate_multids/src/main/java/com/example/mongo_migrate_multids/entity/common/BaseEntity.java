package com.example.mongo_migrate_multids.entity.common;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import com.example.mjg.data.MigratableEntity;

import java.time.Instant;

@Getter
@Setter

public abstract class BaseEntity implements MigratableEntity {

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    @Field("tenant_code")
    private String tenantCode;
}
