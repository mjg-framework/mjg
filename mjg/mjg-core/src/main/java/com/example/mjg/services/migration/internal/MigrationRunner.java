package com.example.mjg.services.migration.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.BaseMigrationException;
import com.example.mjg.exceptions.CardinalityViolationException;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.services.migration.internal.fault_tolerance.MigrationProgressManager;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.reflective.RForEachRecordFrom;
import com.example.mjg.services.migration.internal.reflective.RMatchWith;
import com.example.mjg.services.migration.internal.reflective.RMigrationUtils;
import com.example.mjg.services.migration.internal.reflective.RTransformAndSaveTo;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.storage.MigrationRegistry;
import com.example.mjg.utils.DataStoreReflection;
import lombok.Getter;

public class MigrationRunner {
    @Getter
    private final DataStoreRegistry storeRegistry;

    @Getter
    private final MigrationRegistry migrationRegistry;

    @Getter
    private final String migrationFQCN;

    private final Object migrationInstance;

    private final Class<?> migrationClass;

    private final ForEachRecordFrom forEachRecordFrom;
    private final List<MatchWith> matchWiths;
    private final TransformAndSaveTo transformAndSaveTo;

    private final RForEachRecordFrom rForEachRecordFrom;
    private final RTransformAndSaveTo rTransformAndSaveTo;
    private final List<RMatchWith> rMatchWiths;

    private final RMigrationUtils rMigrationUtils;

    private final MigrationProgressManager migrationProgressManager;

    public MigrationRunner(
        DataStoreRegistry storeRegistry,
        MigrationRegistry migrationRegistry,
        MigrationProgressManager migrationProgressManager,
        String migrationFQCN
    ) {
        this.storeRegistry = storeRegistry;
        this.migrationRegistry = migrationRegistry;
        this.migrationProgressManager = migrationProgressManager;
        this.migrationFQCN = migrationFQCN;
        this.migrationInstance = migrationRegistry.get(migrationFQCN);
        this.migrationClass = migrationInstance.getClass();
        
        this.forEachRecordFrom = migrationClass.getAnnotationsByType(ForEachRecordFrom.class)[0];
        this.matchWiths = buildMatchingPlan(
            migrationClass.getAnnotationsByType(MatchWith.class)
        );
        this.transformAndSaveTo = migrationClass.getAnnotationsByType(TransformAndSaveTo.class)[0];

        this.rForEachRecordFrom = new RForEachRecordFrom(
            migrationClass,
            forEachRecordFrom,
            new DataStoreReflection(this.forEachRecordFrom.value())
        );

        this.rTransformAndSaveTo = new RTransformAndSaveTo(
            migrationClass,
            transformAndSaveTo,
            new DataStoreReflection(this.transformAndSaveTo.value())
        );

        this.rMatchWiths = this.matchWiths.stream()
            .map(matchWith -> new RMatchWith(
                migrationClass,
                matchWith,
                new DataStoreReflection(matchWith.value()))
            )
            .toList();

        this.rMigrationUtils = new RMigrationUtils(storeRegistry, migrationClass, migrationInstance, rForEachRecordFrom, rMatchWiths, rTransformAndSaveTo);
    }

    public void restoreProgress(MigrationProgress migrationProgress) {
        this.migrationProgressManager.initialize(migrationProgress);
    }

    public void run() {
        DataStore<MigratableEntity, Object, Object> inputStore = getDataStore(
            rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
        );
        runInternal(inputStore, Map.of());
    }

    public void runWithRecordIdIn(Set<Object> recordIds) {
        DataStore<MigratableEntity, Object, Object> inputStore = getDataStore(
            rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
        );
        runInternal(
            inputStore,
            inputStore.getFiltersByIdIn(recordIds)
        );
    }

    private void runInternal(
        DataStore<MigratableEntity, Object, Object> inputStore,
        Map<Object, Object> filters
    ) {
        final int INPUT_BATCH_SIZE = rForEachRecordFrom.getForEachRecordFrom().batchSize();
        
        DataPage<MigratableEntity, Object, Object> inputPage = inputStore.getFirstPageOfRecordsWithFilter(
            filters,
            INPUT_BATCH_SIZE
        );
        while (inputPage.getSize() > 0) {
            // Filter out those that are already migrated
            List<MigratableEntity> records = inputPage.getRecords();

            Set<Object> inputRecordIds = records
                .stream()
                .map(MigratableEntity::getMigratableId)
                .collect(Collectors.toCollection(HashSet::new));

            migrationProgressManager.excludeSuccessfullyMigratedRecordIds(
                this.migrationFQCN,
                inputRecordIds
            );

            List<MigratableEntity> records = inputPage.getRecords()
                .stream()
                .filter()
            // Process
            migrateRecords(inputPage.getRecords());
            // Next page
            inputPage = inputStore.getNextPageOfRecordsAfter(inputPage);
        }
    }

    private void migrateRecords(List<MigratableEntity> inputRecords) {
        // Setup
        @SuppressWarnings("unchecked")
        DataStore<MigratableEntity, Object, Object> outputStore = (DataStore<MigratableEntity, Object, Object>) storeRegistry
            .get(rTransformAndSaveTo.getDataStoreReflection().getStoreClass().getCanonicalName());
        
        List<RecordProcessingContext> inputContexts = inputRecords
            .stream()
            .map(RecordProcessingContext::new)
            .toList();

        // Match and reduce
        rMatchWiths.forEach(rMatchWith -> {
            matchAndReduceRecordsPerMatching(rMatchWith, inputContexts);
        });

        // Transform
        // TODO: Could use parallelStream() here
        Cardinality transformCardinality = rTransformAndSaveTo.getTransformAndSaveTo().cardinality();
        Stream<MigratableEntity> outputRecordStream = inputContexts.stream()
            .map(ctx -> {
                List<MigratableEntity> outputRecords = rMigrationUtils
                    .callTransformMethod(ctx.getAggregates(), ctx.getRecord());

                try {
                    CardinalityCheck.checkConformant(
                        migrationFQCN,
                        rTransformAndSaveTo.getTransformAndSaveTo().toString(),
                        transformCardinality,
                        outputRecords.size()
                    );
                } catch (CardinalityViolationException e) {

                }

                return outputRecords;
            })
            .flatMap(List::stream);

        // Save
        final int BATCH_SIZE = rTransformAndSaveTo.getTransformAndSaveTo().batchSize();
        List<MigratableEntity> outputRecordsBatch = new ArrayList<>(BATCH_SIZE);
        outputRecordStream.forEach(outputRecord -> {
            outputRecordsBatch.add(outputRecord);
            if (outputRecordsBatch.size() >= BATCH_SIZE) {
                try {
                    // Save
                    outputStore.saveMultiple(outputRecordsBatch);
                } catch (DuplicateDataException e) {

                } catch (BaseMigrationException e) {
                }
                // Reset batch
                outputRecordsBatch.clear();
            }
        });

        // Last batch, if any
        if (!outputRecordsBatch.isEmpty()) {
            try {
                outputStore.saveMultiple(outputRecordsBatch);
            } catch (DuplicateDataException e) {
                throw sneakyThrow(e);
            }
            outputRecordsBatch.clear();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void matchAndReduceRecordsPerMatching(RMatchWith rMatchWith, List<RecordProcessingContext> recordContexts) {
        DataStore<? extends MigratableEntity, ?, ?> store = storeRegistry
            .get(rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName());
        final int BATCH_SIZE = rMatchWith.getMatchWith().batchSize();

        final Cardinality cardinality = rMatchWith.getMatchWith().cardinality();
        
        // Group records by matching filter sets
        Map<Map<Object, Object>, List<RecordProcessingContext>> recordContextsByFiltersMap = recordContexts.stream()
            .collect(
                Collectors.groupingBy(recordContext -> rMigrationUtils.callMatchingMethod(rMatchWith, recordContext.getRecord()))
            );
        
        // For each filter set, query the datastore, get matching records,
        // and reduce each input record on those matching records.
        recordContextsByFiltersMap
            .forEach((filters, inputRecordContexts) -> {
                DataPage<? extends MigratableEntity, ?, ?> matchingPage = store
                    .getFirstPageOfRecordsWithFilter((Map) filters, BATCH_SIZE);

                long numMatchingRecords = 0;
                final AtomicBoolean isReductionInitialized = new AtomicBoolean(false);
                while (matchingPage.getSize() > 0) {
                    List<MigratableEntity> moreMatchingRecords = (List<MigratableEntity>) matchingPage.getRecords();
                    numMatchingRecords += moreMatchingRecords.size();

                    try {
                        CardinalityCheck.checkConformantInProgress(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);
                    } catch (CardinalityViolationException e) {
                        throw sneakyThrow(e);
                    }

                    // TODO: Parallel streaming here might improve speed
                    inputRecordContexts.forEach(ctx -> {
                        try {
                            if (!isReductionInitialized.get()) {
                                rMigrationUtils.callStartReductionMethod(ctx.getAggregates());
                            }
                            rMigrationUtils.callReduceMethod(rMatchWith, ctx.getAggregates(), moreMatchingRecords);
                        } catch (Exception e) {
                            // report
                        }
                    });

                    matchingPage = store.getNextPageOfRecordsAfter((DataPage) matchingPage);

                    isReductionInitialized.set(true);
                }

                try {
                    CardinalityCheck.checkConformant(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);
                } catch (CardinalityViolationException e) {
                    throw sneakyThrow(e);
                }
            });
    }

    private static List<MatchWith> buildMatchingPlan(MatchWith[] matchWiths) {
        return Arrays.asList(matchWiths).stream()
            .sorted(Comparator.comparing(MigrationRunner::prioritizedOrderingOfMatchWith))
            .toList();
    }

    /**
     * the lower, the more priority
     */
    private static int prioritizedOrderingOfMatchWith(MatchWith matchWith) {
        int prioritizedOrdering = 0;
        Cardinality cardinality = matchWith.cardinality();
        if (cardinality == Cardinality.EXACTLY_ONE || cardinality == Cardinality.ONE_OR_MORE) {
            // For these cardinalities, error will occur when data is missing.
            // Therefore, we need to resolve these matchings first, so that
            // when there is error and we need to stop, we could stop earlier.
            prioritizedOrdering = 1;
        }
        return prioritizedOrdering;
    }

    @SuppressWarnings("unchecked")
    private DataStore<MigratableEntity, Object, Object> getDataStore(String fqcn) {
        return (DataStore<MigratableEntity, Object, Object>) storeRegistry
            .get(fqcn);
    }
}
