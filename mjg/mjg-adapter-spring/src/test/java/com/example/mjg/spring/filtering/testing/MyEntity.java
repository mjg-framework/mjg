package com.example.mjg.spring.filtering.testing;

import com.example.mjg.data.MigratableEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class MyEntity implements MigratableEntity {
    Integer id;

    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "MyEntity(id=" + id + ")";
    }
}
