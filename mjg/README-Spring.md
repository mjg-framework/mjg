# mjg usage for Spring Data repositories e.g. JPA, MongoDB etc.

- [mjg usage for Spring Data repositories e.g. JPA, MongoDB etc.](#mjg-usage-for-spring-data-repositories-eg-jpa-mongodb-etc)
  - [Goal](#goal)
  - [Simple use case](#simple-use-case)
    - [1. Define Entities](#1-define-entities)
    - [2. Define Spring Repositories](#2-define-spring-repositories)
    - [3. Define a mjg DataStore linked to each Spring repository](#3-define-a-mjg-datastore-linked-to-each-spring-repository)
    - [4. Define a Migration class](#4-define-a-migration-class)

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

Or more precisely:  
**For each `oldArea` from `srcAreaRepository`,**  
**create `newArea` with same content as `oldArea` except id,**  
**then save `newArea` to `destAreaRepository`**.


The new way using `mjg`:

## Simple use case

### 1. Define Entities

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
}
```

### 2. Define Spring Repositories

Make sure your repository interface extends `MigratableMongoRepository`
(for MongoDB) or `MigratableSpringRepository` (for JPA or anything else).

You can declare any method. However, only some methods will be
usable in the filter step (below) - they are the methods
that accept a `Pageable` as the last argument, and return
a `Page`.

For example, here are the class for `srcAreaRepository`.

```java
public interface AreaRepository extends MigratableMongoRepository<AreaEntity, String> {
    Page<AreaEntity> findAllByAreaCodeIn(
        Collection<String> areaCode,
        Pageable pageable
    );
}
```

`destAreaRepository` is similar. (In fact, since
they are so similar, you could define a base interface
e.g. `BaseAreaRepository`, then have `SrcAreaRepository`
and `DestAreaRepository` inherit from it.

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

### 4. Define a Migration class

Now that we have the entities and stores ready,
write the migration logic. Here is the example
code followed by an explanation.

```java
import java.util.List;
import java.util.Map;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.exceptions.DuplicateDataException;

@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class,
)
@TransformAndSaveTo(
    value = DestAreaStore.class,
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
is arbitrary. Migration logic is wrapped into
*migration classes* like this one.

Each *migration class* must have the `@Migration`
annotation applied. The `@ForEachRecordFrom` and
`TransformAndSaveTo` annotations specify which
**datastores (not repositories)** to copy from
and save to.

Here, since we want to copy data from
`srcAreaRepository` to `destAreaRepository`,
we wrapped them into `srcAreaStore` and
`destAreaStore`, then pass those stores into
the annotations of our migration class, hence
the lines

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class,
)
@TransformAndSaveTo(
    value = DestAreaStore.class,
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
 * For now just return null
 */
public List<AreaEntity> handleDuplicate(
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
run the migration logic as follows:

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
