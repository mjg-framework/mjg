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
import java.util.List;

@Document(value = "ambiguous_clusterings")
@Getter
@Setter
public class AmbiguousClusteringEntity {
    @Id
    public String id;

    @Field(value = "classA")
    public String classA;

    @Field(value = "relevantColumnsA")
    public List<String> relevantColumnsA;

    @Field(value = "classB")
    public String classB;

    @Field(value = "relevantColumnsB")
    public List<String> relevantColumnsB;

    @Field(value = "recordsA")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public List<Document> recordsA;

    @Field(value = "recordB")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public Document recordB;

    @Field(value = "status")
    public String status;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;
}
