package com.example.mongo_migrate_multids.entity.self;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(value = "insert_errors")
@Getter
@Setter
public class InsertErrorEntity {
    @Id
    public String id;

    @Field(value = "message")
    public String message;

    @Field(value = "classB")
    public String classB;

    @Field(value = "classBPrime")
    public String classBPrime;

    @Field(value = "recordB")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public Document recordB;

    @Field(value = "recordBPrime")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public Document recordBPrime;

    @Field(value = "status")
    public String status;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;
}
