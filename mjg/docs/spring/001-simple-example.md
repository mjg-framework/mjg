# Simple Example

- [Simple Example](#simple-example)
  - [Goal](#goal)
  - [Simple Example with mjg](#simple-example-with-mjg)
    - [1. Define Entities](#1-define-entities)
    - [2. Define Spring Repositories](#2-define-spring-repositories)
    - [3. Define a mjg DataStore linked to each Spring repository](#3-define-a-mjg-datastore-linked-to-each-spring-repository)
    - [4. Define a Migration class](#4-define-a-migration-class)
    - [5. Run mjg](#5-run-mjg)
  - [See also](#see-also)

## Goal

For example, you want to migrate table
`areas` from one database to another
(suppose they are both MongoDB, but
it could be SQL-based as well).

**Without mjg (i.e. with only Spring**
**repositories)**, you would need
something like

```java
for (AreaEntity oldArea : srcAreaRepository.findAll()) {
    AreaEntity newArea = new AreaEntity();
    // copy oldArea fields into newArea
    // maybe just don't copy the ID field
    // then
    destAreaRepository.save(newArea);
};
```

One sentence to describe it:  
**For each `oldArea` from `srcAreaRepository`,**  
**copy to `destAreaRepository`**.

Or more precisely:  v
**For each `oldArea` from `srcAreaRepository`,**  
**create `newArea` with same content as `oldArea` except id,**  
**then save `newArea` to `destAreaRepository`**.


The new way using `mjg`:

## Simple Example with mjg

We are going to create a `mjg` *migration*
to solve this instead. The purpose of this
migration is, sure enough, to *migrate* data
from `SrcAreaRepository` to `DestAreaRepository`.

Writing a *migration* is simple - we only
need to write a *Migration class* - but first,
we have to prepare some stuff:

### 1. Define Entities

You have done this if you've used Spring JPA
or Spring Data MongoDB to define entities that
represent some tables/collections.

The procedure is the same, with one tiny
addition:

Make sure your entity implements the `Migratable` interface,
and implement the methods required by that interface.

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.example.mjg.data.MigratableEntity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Document(value = "areas")
public class AreaEntity implements MigratableEntity {
    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "Area(id=" + id + ")";
    }

    @Id
    public String id;

    @Field(value = "area_code")
    public String areaCode;

    @Field(value = "area_name")
    public String areaName;

    @Field(value = "description")
    public String description;

    @Field(value = "order_no")
    public Object orderNo;

    @Field(value = "average_temperature")
    public Double averageTemperature;
}
```

### 2. Define Spring Repositories

Again, you should be familiar with this already.
There is also a tiny addition:

Make sure your repository interface extends `MigratableMongoRepository`
(for MongoDB) or `MigratableSpringRepository` (for JPA or anything else).

```java
import com.example.mjg.spring.repositories.MigratableSpringRepository;
import com.example.mjg.spring.mongo.repositories.MigratableMongoRepository;
```

You can declare any method. However, only some methods
will be usable in the filter step
([in the next article](./002-matching-and-averaging.md)) -
they are the methods that accept a `Pageable` as the
last argument, and return a `Page`. (You'll later see how
the filter step would look like)

For example, here are the class for `srcAreaRepository`.

```java
public interface AreaRepository
extends MigratableMongoRepository<AreaEntity, String> {
    // Could be used in the filter step
    Page<AreaEntity> findAllByAreaCodeIn(
        Collection<String> areaCode,
        Pageable pageable
    );
}
```

`destAreaRepository` is similar. (In fact, since
they are so similar, you could define a base interface
e.g. `BaseAreaRepository`, then have `SrcAreaRepository`
and `DestAreaRepository` inherit from it.)

**If it's JPA (SQL) or some other kind of repository,**
extends `MigratableSpringRepository` instead.

### 3. Define a mjg DataStore linked to each Spring repository

Make sure it has a member field named `repository`,
which is an instance of the real repository under
the hood of the datastore.

Using `@Component`, Spring would automatically
inject the repository for us.

For example, the datastore for `SrcAreaRepository`:

```java
import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SrcAreaStore
extends MongoRepositoryStore<AreaEntity, String>
{
    @Getter
    private final SrcAreaRepository repository;
}
```

Below is the example for `DestAreaRepository`.
The only differences are `SrcXXX` -> `DestXXX`.

```java
import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DestAreaStore
extends MongoRepositoryStore<AreaEntity, String>
{
    @Getter
    private final DestAreaRepository repository;
}
```

Again, you could also define a common
`abstract class` and have the two inherit
from it.

**For JPA,** use `SpringRepositoryStore`.

```java
import com.example.mjg.spring.stores.SpringRepositoryStore;
```

### 4. Define a Migration class

Now that we have the entities and stores ready,
write the migration logic in a *Migration class*.
Here is the example code followed by an explanation.

```java
import java.util.List;
import java.util.Map;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.exceptions.DuplicateDataException;

@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@TransformAndSaveTo(
    value = DestAreaStore.class
)
public class MigrateAreasFromSrcToDest {
    public void startReduction(
        AreaEntity inputRecord,
        Map<String, Object> aggregates
    ) {}

    public List<AreaEntity> transform(
        Map<String, Object> aggregates,
        AreaEntity oldArea
    ) {
        AreaEntity newArea = new AreaEntity();
        newArea.setAreaCode(oldArea.getAreaCode());
        newArea.setAreaName(oldArea.getAreaName());
        newArea.setDescription(oldArea.getDescription());
        newArea.setOrderNo(oldArea.getOrderNo());
        newArea.setAverageTemperature(oldArea.getAverageTemperature());
        return List.of(newArea);
    }

    public List<AreaEntity> handleDuplicate(
        DuplicateDataException exception,
        AreaEntity inputRecord,
        List<AreaEntity> outputRecords,
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        // Do not handle duplicate error
        return null;
    }
}
```

The class name (here `MigrateAreasFromSrcToDest`)
is arbitrary - you can name it whatever you want.
Migration logic is wrapped into
*migration classes* like this one.

Each *migration class* must have the `@Migration`
annotation applied. The `@ForEachRecordFrom` and
`TransformAndSaveTo` annotations specify which
**datastores (not repositories)** to copy from
and save to. (That's why we needed to create
datastores that depend on repositories under
the hood earlier.)

Here, since we want to copy data from
`srcAreaRepository` to `destAreaRepository`,
we wrapped them into `srcAreaStore` and
`destAreaStore`, then pass those stores into
the annotations of our migration class, hence
the lines

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@TransformAndSaveTo(
    value = DestAreaStore.class
)
public class MigrateAreasFromSrcToDest {
    // ...
}
```

Let's call `oldArea` an *input record*,
`newArea` an *output record*. The records'
types must be the same as the entity types
of their stores/repositories. Here, since
both stores/repositories have the entity
type `AreaEntity`, so we have:

    SrcEntity = DestEntity = "AreaEntity"

**Each migration class must have the**
**following methods at a minimum:**

```java
/**
 * We will learn about this later.
 * Now we don't use this method, so do nothing inside here.
 */
public void startReduction(
    SrcEntity inputRecord,
    Map<String, Object> aggregates
) {}

/**
 * This method converts the input record to
 * one or more output records, and return the
 * list of the output records.
 * 
 * Here we don't use "aggregates" yet.
 */
public List<DestEntity> transform(
    Map<String, Object> aggregates,
    SrcEntity inputRecord
) {
    DestEntity outputRecord = convertInputToOutputRecord(inputRecord);
    return List.of(outputRecord);
}

/**
 * We will learn about this later.
 * For now just return null.
 */
public List<DestEntity> handleDuplicate(
    // import com.example.mjg.exceptions.DuplicateDataException;
    DuplicateDataException exception,
    SrcEntity inputRecord,
    List<DestEntity> outputRecords,
    SrcStore srcStore,
    DestStore destStore,
    // import com.example.mjg.storage.DataStoreRegistry;
    DataStoreRegistry dataStoreRegistry
) {
    return null;
}
```

There we have our migration class! `mjg` will
run **the migration process** as follows:

1. It pulls records from the source store
    denoted by `@ForEachRecordFrom`.

2. For each such record, it calls `startReduction`,
    then calls `transform` to get the output record(s).

3. Save all of the output record(s) to the
    destination store denoted by `@TransformAndSaveTo`.

4. If all the output records are saved successfully,
    process the next input record from step 1.

    If one of them fails, move to step 5.

    **Special case:** If that error is from a data
    duplicate exception, e.g. thrown when the new
    record(s) violate a unique constraint, the
    framework will call `handleDuplicate` with:

    - the exception itself
    - the input record
    - the output record(s) as returned by `transform`
    - the input and output stores
    - a datastore registry (more on this later)

    If `handleDuplicate` chooses to not handle the
    duplicate error, it should return `null`, then
    the framework would move to step 5.
    
    Otherwise, `handleDuplicate` should return a new
    list of output record(s) to save to the dest
    store instead of `outputRecords` (the previous
    output record(s) returned by `transform`).

    If the new output records still fail to be saved,
    regardless of it being due to data duplication or
    not, move to step 5.

5. Save the input record to the list of failed input
    records, along with the exception that caused the
    failure.
    
    Then, process the next input record from step 1.

One sentence to summarize the intent of our example:  
**For each `oldArea` from `srcAreaStore`,**  
**create `newArea` with same content as `oldArea` except id,**  
**then save `newArea` to `destAreaStore`,**  
**handle any error in the process**.

It is the built-in, ready-to-use error handling
that makes `mjg` a safe, fault-tolerant and
convenient alternative to just move records from
one repository to another manually.

### 5. Run mjg

After defining all migrations, here is how
to run them with mjg.

Define a Service that has the stores autowired,
then create a data store registry containing
those stores, pass to `mjg`'s `MigrationService`,
then just call `.run()`.

Here is the example code:

```java
import com.example.mjg.storage.DataStoreRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.mjg.services.migration.MigrationService;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.utils.ObjectMapperFactory;

@Slf4j
@Service
public class MyMigrateService {
    public static final String MIGRATION_PROGRESS_FILE_PATH = "migrate-tw-progress.json";

    private final MigrationService migrationService;
    private final DataStoreRegistry dataStoreRegistry;

    public MyMigrateService(
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
    ) {
        dataStoreRegistry = new DataStoreRegistry();

        dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
        dataStoreRegistry.set(DestAreaStore.class, destAreaStore);

        migrationService = new MigrationService(dataStoreRegistry);
    }

    public void run() {
        MigrationProgress migrationProgress = new MigrationProgress();

        migrationService.run(migrationProgress);
    }
}
```

Notice that we have to pass a `migrationProgress` instance
to the `migrationService.run`. That is, after each run,
`mjg` will store the progress (which records have been
migrated, which failed, what exceptions caused the
failures...) into a JSON-serializable object. You could
save this into a JSON file, and load it to `migrationService.run`
every time you call it.

    (progress.json) -> migrationService.run() -> (progress.json) -> ...

Here is an example that do exactly that, so the
migration progress will be truly fault-tolerant -
when something fails, you can always view the
exceptions, fix broken stuff, and resume the
migration process!

```java
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.services.migration.MigrationService;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.utils.ObjectMapperFactory;

@Slf4j
@Service
public class MyMigrateService {
    public static final String MIGRATION_PROGRESS_FILE_PATH = "migrate-tw-progress.json";

    private final MigrationService migrationService;
    private final DataStoreRegistry dataStoreRegistry;

    public MyMigrateService(
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
    ) {
        dataStoreRegistry = new DataStoreRegistry();

        dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
        dataStoreRegistry.set(DestAreaStore.class, destAreaStore);

        migrationService = new MigrationService(dataStoreRegistry);

        migrationService.removeAllProgressPersistenceCallbacks();

        migrationService.addProgressPersistenceCallback(MyMigrateService::saveMigrationProgress);
    }

    public void run() {
        MigrationProgress migrationProgress = loadMigrationProgress();

        migrationService.run(migrationProgress);
    }

    //////// METHODS TO LOAD AND SAVE MIGRATION PROGRESS //////////////
    /////////////////////// FROM A JSON FILE //////////////////////////

    // Yes, it's just serializing/deserializing JSON

    private static MigrationProgress loadMigrationProgress() {
        ObjectMapper objectMapper = ObjectMapperFactory.get();
        MigrationProgress migrationProgress;
        boolean fileExists;
        try {
            migrationProgress = objectMapper.readValue(new File(MIGRATION_PROGRESS_FILE_PATH), MigrationProgress.class);
            fileExists = true;
        } catch (FileNotFoundException e) {
            fileExists = false;
            log.warn("No previous migration progress saved in " + MIGRATION_PROGRESS_FILE_PATH);
            log.warn("Starting fresh.");
            migrationProgress = new MigrationProgress();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load migration progress from file: " + MIGRATION_PROGRESS_FILE_PATH, e);
        }

        // SAFETY MEASURE: Backup the old progress file before overwriting it later
        //                 with saveMigrationProgress() (OPTIONAL)
        if (fileExists) {
            try {
                Files.copy(
                    Paths.get(MIGRATION_PROGRESS_FILE_PATH),

                    Paths.get(
                        MIGRATION_PROGRESS_FILE_PATH
                            + "."
                            + migrationProgress.getMetadata().getTimestamp().toEpochSecond(
                            ZoneOffset.of("Z")
                        )
                            + ".bak.json"
                    ),

                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                log.warn("Could not backup previous migration progress file.");
            }
        }
        // END SAFETY MEASURE

        return migrationProgress;
    }

    private static void saveMigrationProgress(MigrationProgress migrationProgress) {
        System.out.println("============= SAVING PROGRESS ================");
        BufferedWriter writer = null;
        try {
            FileWriter fileWriter = new FileWriter(MIGRATION_PROGRESS_FILE_PATH, false);
            writer = new BufferedWriter(fileWriter);

            ObjectMapper objectMapper = ObjectMapperFactory.get();
            String jsonString = objectMapper.writeValueAsString(migrationProgress);
            writer.write(jsonString);

            System.out.println("Migration progress successfully saved to: " + MIGRATION_PROGRESS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving migration progress to file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing BufferedWriter: " + e.getMessage());
                }
            }
        }
    }
}
```

## See also

- [Home](./README.md)
- [Previous: Overview](./000-overview.md)
- [Next: Advanced Example - Matching and Averaging](./002-matching-and-averaging.md)
