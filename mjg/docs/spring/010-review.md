# Review

- [Review](#review)
  - [Feature Review](#feature-review)
  - [Checklist](#checklist)
  - [Writing a Migration Class](#writing-a-migration-class)
  - [Inspecting Errors](#inspecting-errors)
  - [Final Thoughts](#final-thoughts)
  - [See also](#see-also)

## Feature Review

Here are the features of `mjg` and some
hints for you to remember everything
we've discussed so far:

- **Automatic batching:** load, match, reduce and save
    records in batch. You could customize the batch size by
    setting `batchSize` in `@ForEachRecordFrom`, `@MatchWith`,
    and `@TransformAndSaveTo`. Default batch size is 512.

    For example:

    ```java
    @ForEachRecordFrom(value = SrcStore.class, batchSize = 128)
    @MatchWith(value = MatchingStore1.class, batchSize = 8)
    @MatchWith(value = MatchingStore2.class, batchSize = 256)
    @TransformAndSaveTo(value = DestStore.class, batchSize = 1)
    ```

- **Progress persistence:** load and save *migration progress*
    as JSON.

    Remember to use `ObjectMapperFactory.get()` to serialize
    or deserialize the *migration progress* as JSON, instead
    of `new ObjectMapper();` as you would normally do with
    Jackson. For example:

    ```java
    import com.example.mjg.utils.ObjectMapperFactory;
    
    ObjectMapper objectMapper = ObjectMapperFactory.get();
    // instead of
    // ObjectMapper objectMapper = new ObjectMapper();

    String jsonString = objectMapper
        .writeValueAsString(migrationProgress);
    ```

    References:

    - [Overview](./000-overview.md#progress-persistence)
    - [Simple Example](./001-simple-example.md#5-run-mjg)
    - [Migration Progress JSON format](./008-migration-progress-json-format.md)

- **Fault tolerance:** specify the retry times and
    delay between retries in `@ForEachRecordFrom`, `@MatchWith`,
    and `@TransformAndSaveTo`.

    For example:

    ```java
    import com.example.mjg.config.ErrorResolution;
    // ...

    @Migration
    @ForEachRecordFrom(
        value = StationStore.class,
        batchSize = 1,
        inCaseOfError = @ErrorResolution(
            // don't retry
            retryTimes = 0, retryDelayInSeconds = 0
        )
    )
    @MatchWith(
        value = IndicatorStore.class,
        batchSize = 1,
        inCaseOfError = @ErrorResolution(
            // retry 2 times, no waiting in between
            retryTimes = 2,
            retryDelayInSeconds = 0
        )
    )
    @TransformAndSaveTo(
        value = StationIndicatorStore.class,
        inCaseOfError = @ErrorResolution(
            // retry 5 times, wait 3 seconds in between
            retryTimes = 5,
            retryDelayInSeconds = 3
        )
    )
    public class MyMigration { /*...*/ }
    ```

    By default, the framework retries only once, 1 seconds
    since the point of failure, i.e. `retryTimes = 1`
    and `retryDelayInSeconds = 1`.

    References:
    - [Overview](./000-overview.md#features)
    - [Fault Tolerance](./007-fault-tolerance.md)

- **Automatic migration ordering/chaining:** `mjg`
    resolves dependencies between migrations and
    decides an order of execution for your migrations
    at compile time.

    Upon runtime, `mjg` will load that precompiled
    execution order, and run your migrations
    accordingly.

    You could also specify the ordering manually.

    [Reference](./006-chaining-migrations.md).

## Checklist

When writing a new migration class,
make sure to check all the following:

1. **Entity:** Define your entity classes that implement the
    `MigratableEntity` interface, and write the
    required methods.

2. **Repository:** (If you're using Spring Data e.g. JPA, MongoDB, etc.)
    Define your Spring repository interfaces that
    extend `MigratableSpringRepository`. **For**
    **MongoDB**, extend `MigratableMongoRepository` instead.

3. **Datastore:** Define your datastore classes. They
    must extend `DataStore`; then you have to implement
    the required abstract methods.
    
    However, if you're using Spring Data, those datastores
    correspond to the previously defined repositories,
    so just extends `SpringRepositoryStore` and make
    sure you have a getter method named `getRepository()`
    which returns the underlying Spring repository. For
    example:

    ```java
    @Component
    @AllArgsConstructor
    public class HouseStore
    extends SpringRepositoryStore<HouseEntity, Long>
    {
        @Getter
        private final HouseRepository repository;
    }
    ```

    **For MongoDB**, extends `MongoRepositoryStore` instead.

4. **Registration** Don't forget to register the newly
    created store into our migration service!

    ```java
    @Slf4j
    @Service
    public class MyMigrateService {
        // ... unchanged ...

        public MyMigrateService(
            SrcAreaStore srcAreaStore,
            DestAreaStore destAreaStore,
            TempStore tempStore,
            HouseStore houseStore,
            // ...
        ) {
            dataStoreRegistry = new DataStoreRegistry();

            dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
            dataStoreRegistry.set(DestAreaStore.class, destAreaStore);
            dataStoreRegistry.set(TempStore.class, tempStore);
            dataStoreRegistry.set(HouseStore.class, houseStore);
            // ...

            migrationService = new MigrationService(dataStoreRegistry);

            // ... unchanged ...
        }

        // ... unchanged ...
    }
    ```

References:

- [Advanced Example - Matching and Averaging](./002-matching-and-averaging.md#final-migration-code)
- Advanced Example - Multiple Matching:
    [here](./004-multiple-matchings.md#our-migration-class)
    and [here](./004-multiple-matchings.md#setup-for-houses)
- Advanced Example - Create Pivot Tables:
    [here](./005-create-pivot-tables.md#set-up)
    and [here](./005-create-pivot-tables.md#the-migration-class)

## Writing a Migration Class

At a minimum, a migration class must have
the following annotations:

```java
@Migration
@ForEachRecordFrom(SrcStore.class)
@TransformAndSaveTo(DestStore.class)
```

And the methods that are always required
are:

```java
public void startReduction(
    SrcEntity inputRecord,
    Map<String, Object> aggregates
);

public List<DestEntity> transform(
    Map<String, Object> aggregates,
    SrcEntity inputRecord
);

public List<DestEntity> handleDuplicate(
    // import com.example.mjg.exceptions.DuplicateDataException;
    DuplicateDataException exception,
    SrcEntity inputRecord,
    List<DestEntity> outputRecords,
    SrcStore srcStore,
    DestStore destStore,
    // import com.example.mjg.storage.DataStoreRegistry;
    DataStoreRegistry dataStoreRegistry
);
```

If you were to specify one or more
matchings, e.g.

```java
@Migration
@ForEachRecordFrom(SrcStore.class)
@MatchWith(MatchingStore1.class)
@MatchWith(MatchingStore2.class)
// ...
@TransformAndSaveTo(DestStore.class)
```

then for each matching, you have
to define these two methods:

```java
public SpringRepositoryFilterSet<MatchingEntity1, MatchingEntity1IDType>
matchWithMatchingStore1(
    SrcEntity inputRecord,
    Map<String, Object> aggregates,
    MatchingStore1 matchingStore1
);

public void reduceFromMatchingStore1(
    Map<String, Object> aggregates,
    List<MatchingEntity1> recordsBatch
);
```

So for `N` more matchings, you need to define
`2N` more methods. Each matching has their
own match-and-reduce methods.

**One important thing you need to do** when
adding more matchings to an existing
migration class, is to check whether
`startReduction()` has initialized
the `aggregates` map properly for
the new matching!

References:

- [Simple Example](./001-simple-example.md)
- [Advanced Example - Matching and Averaging](./002-matching-and-averaging.md)
- [Handling Duplicates](./003-handling-duplicates.md)
- [Advanced Example - Multiple Matching](./004-multiple-matchings.md)
- [Advanced Example - Create Pivot Tables](./005-create-pivot-tables.md)

## Inspecting Errors

See [this article](./008-migration-progress-json-format.md)
for a guide on how to inspect and resolve
errors during the migration process.

If you choose to persist the *migration progress*
in a file, be sure to examine that file
after every run of your migration program,
to make sure everything has worked. If
there are errors, resolve them as instructed
in the aforementioned article, then simply
re-run your migration program.

## Final Thoughts

This is the final article. Congratulations on
having gone through all this!

If you have any questions, feel free to ask
the framework's authors.

## See also

- [Home](./README.md)
- [Previous: Datasource-specific Notes](./009-datasource-specific-notes.md)
