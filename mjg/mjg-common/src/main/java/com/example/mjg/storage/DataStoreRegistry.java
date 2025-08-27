package com.example.mjg.storage;

import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DataStoreRegistry extends AbstractRegistry<DataStore<? extends MigratableEntity, Object, Object>> {
}
