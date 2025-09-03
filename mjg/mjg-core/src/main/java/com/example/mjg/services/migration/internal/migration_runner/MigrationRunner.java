package com.example.mjg.services.migration.internal.migration_runner;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.MigrationErrorInvestigator;
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

    private final MigrationErrorInvestigator migrationErrorInvestigator;

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
            .sorted(Comparator.comparingInt(MatchWith::order))
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

        this.migrationErrorInvestigator = new MigrationErrorInvestigator(migrationProgressManager, this);
    }

    private final DataStore<MigratableEntity, Serializable, DataFilterSet> inputStore;
    private final DataStore<MigratableEntity, Serializable, DataFilterSet> outputStore;



    public void run()
    throws RetriesExhaustedException {
        this.migrationErrorInvestigator.startInBackground();
        this.migrationErrorInvestigator.retryPreviouslyFailedRecords();
        
        DataStore<MigratableEntity, Serializable, DataFilterSet> inputStore = getDataStore(
            rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
        );
        runInternal(inputStore.matchAll());

        migrationErrorInvestigator.join();
        final int numFailures = migrationErrorInvestigator.getNumFailures();
        if (numFailures > 0) {
            throw new RetriesExhaustedException(migrationFQCN + " experienced at least " + numFailures + " failures");
        }
    }

    public void runWithRecordIdIn(Set<Serializable> recordIds) {
        runInternal(inputStore.matchByIdIn(recordIds));
    }

    private void runInternal(DataFilterSet filterSet) {
        final int INPUT_BATCH_SIZE = rForEachRecordFrom.getForEachRecordFrom().batchSize();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rForEachRecordFrom.getForEachRecordFrom().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rForEachRecordFrom.getForEachRecordFrom().inCaseOfError().retryDelayInSeconds())
            .exceptionReporter(
                (e, arg) -> migrationErrorInvestigator.reportFatalError(e)
            )
            .debugContext(
                "While reading records from store: " + rForEachRecordFrom.getDataStoreReflection().getStoreClass().getCanonicalName()
                + "\nwith filterSet: " + filterSet
            );

        var getFirstPageOfRecordsWithFilter = retryLogic.withCallback(
            (Object ignoredArg) -> inputStore.getFirstPageOfRecords(
                filterSet,
                INPUT_BATCH_SIZE
            )
        );
        var getNextPageOfRecordsAfter = retryLogic.withCallback(
            inputStore::getNextPageOfRecords
        );

        DataPage<MigratableEntity, Serializable, DataFilterSet> inputPage;
        try {
            inputPage = getFirstPageOfRecordsWithFilter
                .apply(null);
        } catch (RetriesExhaustedException e) {
            return;
        }

        while (inputPage.getSize() > 0) {
            List<MigratableEntity> originalRecords = inputPage.getRecords();

            Set<Serializable> inputRecordIds = originalRecords
                .stream()
                .map(MigratableEntity::getMigratableId)
                .collect(Collectors.toCollection(HashSet::new));

            {
                // Filter out those that are already migrated, and those that are ignored
                migrationErrorInvestigator.excludeSuccessfullyMigratedRecordIds(inputRecordIds);
                migrationErrorInvestigator.excludeIgnoredRecordIds(inputRecordIds);
            }

            List<MigratableEntity> recordsToMigrate = originalRecords
                .stream()
                .filter(record -> inputRecordIds.contains(record.getMigratableId()))
                .toList();
            
            // Process
            if (!recordsToMigrate.isEmpty()) {
                migrateRecords(recordsToMigrate);
            }
            
            // Next page
            try {
                inputPage = getNextPageOfRecordsAfter.apply(inputPage);
            } catch (RetriesExhaustedException e) {
                return;
            }
        }
    }

    private void migrateRecords(List<MigratableEntity> inputRecords) {
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
    private DataStore<MigratableEntity, Serializable, DataFilterSet> getDataStore(String fqcn) {
        return (DataStore<MigratableEntity, Serializable, DataFilterSet>) storeRegistry
            .get(fqcn);
    }
}
