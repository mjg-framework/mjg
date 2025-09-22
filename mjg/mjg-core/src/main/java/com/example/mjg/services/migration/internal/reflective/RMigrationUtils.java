package com.example.mjg.services.migration.internal.reflective;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.utils.RMethodSignature;
import lombok.extern.slf4j.Slf4j;

import static lombok.Lombok.sneakyThrow;

@Slf4j
public class RMigrationUtils {
    private final DataStoreRegistry dataStoreRegistry;
    private final Class<?> migrationClass;
    private final Object migrationInstance;

    private final RForEachRecordFrom rForEachRecordFrom;
    // private final List<RMatchWith> rMatchWiths;
    private final RTransformAndSaveTo rTransformAndSaveTo;

    public RMigrationUtils(
        DataStoreRegistry storeRegistry,
        Class<?> migrationClass,
        Object migrationInstance,
        RForEachRecordFrom rForEachRecordFrom,
        List<RMatchWith> rMatchWiths,
        RTransformAndSaveTo rTransformAndSaveTo
    ) {
        this.dataStoreRegistry = storeRegistry;
        this.migrationClass = migrationClass;
        this.migrationInstance = migrationInstance;
        this.rForEachRecordFrom = rForEachRecordFrom;
        // this.rMatchWiths = rMatchWiths;
        this.rTransformAndSaveTo = rTransformAndSaveTo;
    }

    public DataFilterSet callMatchingMethod(
        RMatchWith rMatchWith,
        MigratableEntity record,
        Map<String, Object> aggregates
    ) throws Exception {
        String methodName = "matchWith" + rMatchWith.getMatchWith().value().getSimpleName();
        DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>
            matchingStoreInstance = dataStoreRegistry.get(rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName());

        final Method method;
        {
            Method cachedMethod = getCachedMethodByName(methodName);
            if (cachedMethod != null) {
                method = cachedMethod;
            } else {
                method = getMethodBySignatureAndCache(
                    new RMethodSignature(
                        methodName,
                        List.of(
                            rForEachRecordFrom.getDataStoreReflection().getEntityClass(),
                            Map.class,
                            rMatchWith.getDataStoreReflection().getStoreClass()
                        )
                    )
                );
            }
        }

        DataFilterSet filters = null;
        {
            Object rawFilterSet = invokeMethod(
                method,

                record,
                aggregates,
                matchingStoreInstance
            );

            if (rawFilterSet != null) {
                if (rawFilterSet instanceof DataFilterSet realFilterSet) {
                    filters = realFilterSet;
                } else {
                    throw new RuntimeException(
                        "Could not cast return value of matching method to DataFilterSet: "
                        + methodName + " from " + migrationClass.getCanonicalName()
                    );
                }
            }
        }

        return filters;
    }

    public void callStartReductionMethod(
        MigratableEntity record,
        Map<String, Object> aggregates
    ) throws Exception {
        String methodName = "startReduction";

        final Method method;
        {
            Method cachedMethod = getCachedMethodByName(methodName);
            if (cachedMethod != null) {
                method = cachedMethod;
            } else {
                method = getMethodBySignatureAndCache(
                    new RMethodSignature(
                        methodName,

                        List.of(
                            rForEachRecordFrom.getDataStoreReflection().getEntityClass(),
                            Map.class
                        )
                    )
                );
            }
        }

        invokeMethod(
            method,

            record,
            aggregates
        );
    }

    public void callReduceMethod(RMatchWith rMatchWith, Map<String, Object> aggregates, List<MigratableEntity> moreMatchingRecords)
    throws Exception {
        String methodName = "reduceFrom" + rMatchWith.getMatchWith().value().getSimpleName();

        final Method method;
        {
            Method cachedMethod = getCachedMethodByName(methodName);
            if (cachedMethod != null) {
                method = cachedMethod;
            } else {
                method = getMethodBySignatureAndCache(
                    new RMethodSignature(
                        methodName,
                        List.of(Map.class, List.class)
                    )
                );
            }
        }

        invokeMethod(
            method,
            
            aggregates,
            moreMatchingRecords
        );
    }

    public List<MigratableEntity> callTransformMethod(Map<String, Object> aggregates, MigratableEntity oldRecord)
    throws Exception {
        String methodName = "transform";

        final Method method;
        {
            Method cachedMethod = getCachedMethodByName(methodName);
            if (cachedMethod != null) {
                method = cachedMethod;
            } else {
                method = getMethodBySignatureAndCache(
                    new RMethodSignature(
                        methodName,
                        List.of(Map.class, rForEachRecordFrom.getDataStoreReflection().getEntityClass())
                    )
                );
            }
        }

        List<MigratableEntity> records = null;
        {
            Object transformedRecordsRaw = invokeMethod(
                method,
                
                aggregates,
                oldRecord
            );

            if (transformedRecordsRaw == null) {
                records = List.of();
            } else {
                if (transformedRecordsRaw instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<MigratableEntity> castRecords = (List<MigratableEntity>) list;
                    records = castRecords;
                } else {
                    throw new RuntimeException(
                        "Could not cast return value of transform method to List<?>: "
                        + methodName + " from " + migrationClass.getCanonicalName()
                    );
                }
            }
        }

        return records;
    }

    public List<MigratableEntity> callHandleDuplicateMethod(
        DuplicateDataException exception,
        MigratableEntity inputRecord,
        List<MigratableEntity> outputRecordsFromTransform
    ) throws Exception {
        DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>
            inputDataStoreInstance = dataStoreRegistry.get(
                rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
            );

        DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>
            outputDataStoreInstance = dataStoreRegistry.get(
                rTransformAndSaveTo.getDataStoreReflection().getStoreClass().getCanonicalName()
            );

        String methodName = "handleDuplicate";

        final Method method;
        {
            Method cachedMethod = getCachedMethodByName(methodName);
            if (cachedMethod != null) {
                method = cachedMethod;
            } else {
                method = getMethodBySignatureAndCache(
                    new RMethodSignature(
                        methodName,
                        List.of(
                            DuplicateDataException.class,
                            rForEachRecordFrom.getDataStoreReflection().getEntityClass(),
                            List.class,
                            rForEachRecordFrom.getDataStoreReflection().getStoreClass(),
                            rTransformAndSaveTo.getDataStoreReflection().getStoreClass(),
                            DataStoreRegistry.class
                        )
                    )
                );
            }
        }

        List<MigratableEntity> resolvedRecords = null;
        {
            Object resolvedRecordsRaw = invokeMethod(
                method,

                exception,
                inputRecord,
                outputRecordsFromTransform,
                inputDataStoreInstance,
                outputDataStoreInstance,
                dataStoreRegistry
            );

            if (resolvedRecordsRaw == null) {
                resolvedRecords = null;
            } else {
                if (resolvedRecordsRaw instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<MigratableEntity> castRecords = (List<MigratableEntity>) list;
                    resolvedRecords = castRecords;
                } else {
                    throw new RuntimeException(
                        "Could not cast return value of handleDuplicate method to List<?>: "
                        + methodName + " from " + migrationClass.getCanonicalName()
                    );
                }
            }
        }

        return resolvedRecords;
    }

    /**
     * Currently our methods are not overloaded,
     * so identify them by names is fine.
     */
    private final Map<String, Method> methodCache = new HashMap<>();

    private Method getCachedMethodByName(String methodName) {
        return methodCache.get(methodName);
    }

    private Method getMethodBySignatureAndCache(RMethodSignature signature) {
        // Method method;
        // try {
        //     method = migrationClass.getMethod(
        //         signature.getName(),
        //         signature.getParameterTypes().toArray(new Class<?>[0])
        //     );
        // } catch (NoSuchMethodException | SecurityException e) {
        //     throw new RuntimeException(
        //         "Could not extract migration method:\n    "
        //             + signature
        //             + "\nfrom    "
        //             + migrationClass.getCanonicalName(),

        //         e
        //     );
        // }

        // methodCache.put(signature.getName(), method);

        // return method;

        // Now look up by name + parameter count.
        // This is because parameter types may involve generics,
        // and at runtime they may be erased.
        // As a result, overloaded methods, with the same number of parameters, are forbidden.

        Method found = null;
        for (Method m : migrationClass.getMethods()) {
            if (!m.getName().equals(signature.getName())) {
                continue;
            }
            if (m.getParameterCount() != signature.getParameterTypes().size()) {
                continue;
            }

            if (found != null) {
                throw new RuntimeException(
                        "Overloaded methods with name '" + signature.getName()
                                + "' are not supported in class "
                                + migrationClass.getCanonicalName());
            }
            found = m;
        }

        if (found == null) {
            throw new RuntimeException(
                    "Could not extract migration method:\n    "
                            + signature
                            + "\nfrom    "
                            + migrationClass.getCanonicalName());
        }

        methodCache.put(signature.getName(), found);
        return found;
    }

    private Object invokeMethod(Method method, Object... args) {
        try {
            return method.invoke(migrationInstance, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw sneakyThrow(cause); // rethrow as if not wrapped
        } catch (
            IllegalAccessException | IllegalArgumentException e
        ) {
            throw new RuntimeException(
                "Could not invoke migration method:\n    "
                    + method
                    + "\nfrom    "
                    + migrationClass.getCanonicalName(),

                    e
            );
        }
    }
}
