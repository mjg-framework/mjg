package com.example.mjg.services.migration.internal.migration_runner;

import java.util.*;
import java.util.stream.Collectors;

import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MigrationRunner {
    private final DataStoreRegistry storeRegistry;

    private final MigrationRegistry migrationRegistry;

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

    private final MatchAndReduceRunner matchAndReduceRunner;
    private final TransformAndSaveRunner transformAndSaveRunner;

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

        this.inputStore = getDataStore(
            rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
        );

        this.outputStore = getDataStore(
            rTransformAndSaveTo.getDataStoreReflection().getStoreClass().getCanonicalName()
        );

        this.matchAndReduceRunner = new MatchAndReduceRunner(this);
        this.transformAndSaveRunner = new TransformAndSaveRunner(this);
    }

    private final DataStore<MigratableEntity, Object, Object> inputStore;
    private final DataStore<MigratableEntity, Object, Object> outputStore;


    public void restoreProgress(MigrationProgress migrationProgress) {
        this.migrationProgressManager.initialize(migrationProgress);
    }

    public void run()
    throws RetriesExhaustedException {
        DataStore<MigratableEntity, Object, Object> inputStore = getDataStore(
            rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
        );
        runInternal(inputStore, Map.of());
    }

    public void runWithRecordIdIn(Set<Object> recordIds)
    throws RetriesExhaustedException {
        runInternal(
            inputStore,
            inputStore.getFiltersByIdIn(recordIds)
        );
    }

    private void runInternal(
        DataStore<MigratableEntity, Object, Object> inputStore,
        Map<Object, Object> filters
    ) throws RetriesExhaustedException {
        final int INPUT_BATCH_SIZE = rForEachRecordFrom.getForEachRecordFrom().batchSize();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rForEachRecordFrom.getForEachRecordFrom().retries())
            .retryDelayInSeconds(rForEachRecordFrom.getForEachRecordFrom().retryDelayInSeconds())
            .exceptionReporter(
                (e, arg) -> migrationProgressManager.reportFatalError(e)
            )
            .debugContext(
                "While reading records from store: " + rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
                + "\nwith filters: " + filters
            );

        var getFirstPageOfRecordsWithFilter = retryLogic.withCallback(
            (Object ignoredArg) -> inputStore.getFirstPageOfRecordsWithFilter(
                filters,
                INPUT_BATCH_SIZE
            )
        );
        var getNextPageOfRecordsAfter = retryLogic.withCallback(
            inputStore::getNextPageOfRecordsAfter
        );

        DataPage<MigratableEntity, Object, Object> inputPage = getFirstPageOfRecordsWithFilter
            .apply(null);

        while (inputPage.getSize() > 0) {
            // Filter out those that are already migrated
            List<MigratableEntity> originalRecords = inputPage.getRecords();

            Set<Object> inputRecordIds = originalRecords
                .stream()
                .map(MigratableEntity::getMigratableId)
                .collect(Collectors.toCollection(HashSet::new));

            migrationProgressManager.excludeSuccessfullyMigratedRecordIds(
                this.migrationFQCN,
                inputRecordIds
            );

            List<MigratableEntity> recordsToMigrate = originalRecords
                .stream()
                .filter(record -> inputRecordIds.contains(record.getMigratableId()))
                .toList();
            // Process
            migrateRecords(recordsToMigrate);
            // Next page
            inputPage = getNextPageOfRecordsAfter.apply(inputPage);
        }
    }

    private void migrateRecords(List<MigratableEntity> inputRecords)
    throws RetriesExhaustedException {
        List<RecordProcessingContext> inputContexts = matchAndReduceRunner.run(inputRecords);
        transformAndSaveRunner.run(inputContexts);
    }

    private static List<MatchWith> buildMatchingPlan(MatchWith[] matchWiths) {
        return Arrays.stream(matchWiths)
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
