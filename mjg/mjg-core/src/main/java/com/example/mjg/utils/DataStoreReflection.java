package com.example.mjg.utils;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

//@AllArgsConstructor
//@Getter
//public class DataStoreReflection {
//    private final Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> storeClass;
//    private final ParameterizedType rootStoreClass;
//
//    private final Class<? extends MigratableEntity> entityClass;
//
//    private final Class<? extends Serializable> idClass;
//
//    private final Class<? extends DataFilterSet> filterSetClass;
//
//    public DataStoreReflection(Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> storeClass) {
//        this.storeClass = storeClass;
//
//        this.rootStoreClass = (ParameterizedType) ReflectionUtils.getResolvedSuperclassType(
//            storeClass,
//            DataStore.class
//        );
//
//        if (rootStoreClass == null) {
//            throw new RuntimeException("Could not find root store class for " + storeClass);
//        }
//        Type entityType = rootStoreClass.getActualTypeArguments()[0];
//        Type idType = rootStoreClass.getActualTypeArguments()[1];
//        Type filterSetType = rootStoreClass.getActualTypeArguments()[2];
//
//        if (entityType instanceof Class<?> rawEntityClass && MigratableEntity.class.isAssignableFrom(rawEntityClass)) {
//            @SuppressWarnings("unchecked")
//            Class<? extends MigratableEntity> entityClass = (Class<? extends MigratableEntity>) rawEntityClass;
//            this.entityClass = entityClass;
//        } else {
//            throw new RuntimeException("DataStore<T, ID, F> where T is not a Migratable, but " + entityType + " (ID = " + idType + " ; filterSet = " + filterSetType + " ; rootStoreClass = " + rootStoreClass + ")");
//        }
//
//        if (idType instanceof Class<?> rawIdClass && Serializable.class.isAssignableFrom(rawIdClass)) {
//            @SuppressWarnings("unchecked")
//            Class<? extends Serializable> idClass = (Class<? extends Serializable>) idType;
//            this.idClass = idClass;
//        } else {
//            throw new RuntimeException("DataStore<T, ID, F> where ID is not a Serializable, but " + idType + " (T = " + entityType + " ; filterSet = " + filterSetType + ")");
//        }
//
//        if (filterSetType instanceof Class<?> rawFilterSetClass && DataFilterSet.class.isAssignableFrom(rawFilterSetClass)) {
//            @SuppressWarnings("unchecked")
//            Class<? extends DataFilterSet> filterSetClass = (Class<? extends DataFilterSet>) rootStoreClass.getActualTypeArguments()[2];
//            this.filterSetClass = filterSetClass;
//        } else {
//            throw new RuntimeException("DataStore<T, ID, F> where F is not a DataFilterSet, but " + filterSetType + " (T = " + entityType + " ; ID = " + idType + ")");
//        }
//    }
//}


/**
 * Fix by ChatGPT
 */
@AllArgsConstructor
@Getter
public class DataStoreReflection {
    private final Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> storeClass;
    private final ParameterizedType rootStoreClass;

    private final Class<? extends MigratableEntity> entityClass;
    private final Class<? extends Serializable> idClass;
    private final Class<? extends DataFilterSet> filterSetClass;

    public DataStoreReflection(Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> storeClass) {
        this.storeClass = storeClass;

        this.rootStoreClass = (ParameterizedType) ReflectionUtils.getResolvedSuperclassType(
            storeClass,
            DataStore.class
        );

        if (rootStoreClass == null) {
            throw new RuntimeException("Could not find root store class for " + storeClass);
        }

        Type entityType     = rootStoreClass.getActualTypeArguments()[0];
        Type idType         = rootStoreClass.getActualTypeArguments()[1];
        Type filterSetType  = rootStoreClass.getActualTypeArguments()[2];

        this.entityClass     = castType(entityType, MigratableEntity.class, "T", idType, filterSetType);
        this.idClass         = castType(idType, Serializable.class, "ID", entityType, filterSetType);
        this.filterSetClass  = castType(filterSetType, DataFilterSet.class, "F", entityType, idType);
    }

    /**
     * Resolve a Type into a concrete Class, verifying it is assignable to expectedSuper.
     */
    @SuppressWarnings("unchecked")
    private static <X> Class<? extends X> castType(
        Type type,
        Class<X> expectedSuper,
        String role,
        Type other1,
        Type other2
    ) {
        Class<?> rawClass = null;

        if (type instanceof Class<?> c) {
            rawClass = c;
        } else if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            rawClass = c;
        }

        if (rawClass != null && expectedSuper.isAssignableFrom(rawClass)) {
            return (Class<? extends X>) rawClass;
        }

        throw new RuntimeException("DataStore<T, ID, F> where " + role + " is not a "
            + expectedSuper.getSimpleName() + ", but " + type
            + " (others: " + other1 + " ; " + other2 + ")");
    }
}
