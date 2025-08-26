package com.example.mjg.services.storage;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class AbstractRegistry<T> {
    private final Map<String, T> dataStores = new HashMap<>();

    public T get(String requestedFQCN) {
        return dataStores.computeIfAbsent(
                requestedFQCN,
                fqcn -> {
                    try {
                        Class<?> clazz = Class.forName(fqcn);
                        @SuppressWarnings("unchecked")
                        T instance = (T) clazz.getDeclaredConstructor().newInstance();
                        // log.debug("Instantiated singleton class: " + fqcn);
                        return instance;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new IllegalStateException("Could not instantiate class, check constructor: " + fqcn, e);
                    }
                }
        );
    }
}
