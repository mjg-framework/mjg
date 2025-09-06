# Advanced Example - Multiple Matchings

From now on, the articles are generally shorter,
so don't get frustrated!

- [Advanced Example - Multiple Matchings](#advanced-example---multiple-matchings)
  - [Goal](#goal)
  - [Setup for Houses](#setup-for-houses)
  - [Our Migration Class](#our-migration-class)
  - [The Order of Matchings](#the-order-of-matchings)
  - [Remarks](#remarks)
  - [See also](#see-also)

## Goal

Suppose `AreaEntity` now has a new field
named `numHouses`. So we have **two matchings**
for each input area record - the matching
temperatures, and the matching houses.

First, modify our `AreaEntity` class.

```java
@Getter
@Setter
@Document(value = "areas")
public class AreaEntity implements MigratableEntity {
    // ... unchanged ...

    @Field(value = "num_houses")
    public Long numHouses;
}
```

## Setup for Houses

Just like before. Now to make it interesting,
suppose the houses are stored in an
SQL table instead of a MongoDB collection.

1. The entity:

    ```java
    @Getter
    @Setter
    @Entity
    public class HouseEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String areaCode;
        
        private String name;
    }
    ```

2. The Spring JPA repository:

    ```java
    import com.example.mjg.spring.repositories.MigratableSpringRepository;

    public interface HouseRepository extends MigratableSpringRepository<HouseEntity, Long> {
        // Could be used in the filter step
        Page<HouseEntity> findAllByAreaCodeIn(
            Collection<String> areaCode,
            Pageable pageable
        );
    }
    ```

3. The datastore:

    ```java
    import com.example.mjg.spring.stores.SpringRepositoryStore;
    import lombok.Getter;
    import lombok.AllArgsConstructor;
    import org.springframework.stereotype.Component;

    @Component
    @AllArgsConstructor
    public class HouseStore
    extends SpringRepositoryStore<HouseEntity, Long>
    {
        @Getter
        private final HouseRepository repository;
    }
    ```

4. Register the new store in our migration service:

    ```java
    @Slf4j
    @Service
    public class MyMigrateService {
        // ... unchanged ...

        public MyMigrateService(
            SrcAreaStore srcAreaStore,
            DestAreaStore destAreaStore,
            TempStore tempStore,
            HouseStore houseStore,      // <-- new store
        ) {
            dataStoreRegistry = new DataStoreRegistry();

            dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
            dataStoreRegistry.set(DestAreaStore.class, destAreaStore);
            dataStoreRegistry.set(TempStore.class, tempStore);
            dataStoreRegistry.set(HouseStore.class, houseStore); // <-- new store

            migrationService = new MigrationService(dataStoreRegistry);

            // ... unchanged ...
        }

        // ... unchanged ...
    }
    ```

## Our Migration Class

Take the [migration code from the previous article](./002-matching-and-averaging.md#final-migration-code)
and add a new `@MatchWith`, new methods `matchWithXXX` and
`reduceFromXXX`, then a few minor modifications in
`startReduction` and `transform`.

As simple as that.

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@MatchWith(
    value = TempStore.class
)
// New matching
@MatchWith(
    value = HouseStore.class
)
@TransformAndSaveTo(
    value = DestAreaStore.class
)
public class MigrateAreasFromSrcToDest {
    /* MODIFIED */
    public void startReduction(
        AreaEntity inputRecord,
        Map<String, Object> aggregates
    ) {
        aggregates.put("sum", (double) 0.0);
        aggregates.put("count", (long) 0);
        aggregates.put("numHouses", (long) 0);    // <-- add this
    }

    /* unchanged */
    public SpringRepositoryFilterSet<TempEntity, String>
    matchWithTempStore(
        AreaEntity inputAreaRecord,
        Map<String, Object> aggregates,     // unused
        TempStore tempStore                 // unused
    ) {
        String areaCode = inputAreaRecord.getAreaCode();
        if (areaCode == null || areaCode.isEmpty()) {
            return null;
        }

        return SpringRepositoryFilterSet.of(
            TempRepository::findAllByAreaCodeIn,
            Set.of(areaCode)
        );
    }

    /* unchanged */
    public void reduceFromTempStore(
        Map<String, Object> aggregates,
        List<TempEntity> tempRecordsBatch
    ) {
        double sum = (double) aggregates.get("sum");
        long count = (long) aggregates.get("count");
        for (TempEntity t : tempRecordsBatch) {
            sum += t.getValue();
        }
        count += tempRecordsBatch.size();

        aggregates.put("sum", sum);
        aggregates.put("count", count);
    }

    /* ADDED */
    public SpringRepositoryFilterSet<HouseEntity, Long>
    matchWithHouseStore(
        AreaEntity inputAreaRecord,
        Map<String, Object> aggregates,     // unused
        HouseStore houseStore               // unused
    ) {
        String areaCode = inputAreaRecord.getAreaCode();
        if (areaCode == null || areaCode.isEmpty()) {
            return null;
        }

        return SpringRepositoryFilterSet.of(
            HouseRepository::findAllByAreaCodeIn,
            Set.of(areaCode)
        );
    }

    /* ADDED */
    public void reduceFromHouseStore(
        Map<String, Object> aggregates,
        List<HouseEntity> houseRecordsBatch
    ) {
        long numHouses = (long) aggregates.get("numHouses");
        numHouses += houseRecordsBatch.size();
        aggregates.put("numHouses", numHouses);
    }

    /* MODIFIED */
    public List<AreaEntity> transform(
        Map<String, Object> aggregates,
        AreaEntity oldArea
    ) {
        double sum = (double) aggregates.get("sum");
        long count = (long) aggregates.get("count");
        double avg;
        if (count == 0)     avg = 0.0;
        else                avg = sum / count;

        /* ADDED PART */
        long numHouses = (long) aggregates.get("numHouses");
        /* END ADDED PART */

        AreaEntity newArea = new AreaEntity();
        newArea.setAreaCode(oldArea.getAreaCode());
        newArea.setAreaName(oldArea.getAreaName());
        newArea.setDescription(oldArea.getDescription());
        newArea.setOrderNo(oldArea.getOrderNo());
        newArea.setAverageTemperature(avg);
        newArea.setNumHouses(numHouses);        // <-- add this also
        return List.of(newArea);
    }

    /* unchanged */
    public List<AreaEntity> handleDuplicate(
        DuplicateDataException exception,
        AreaEntity inputRecord,
        List<AreaEntity> outputRecords,
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        return null;
    }
}
```

Now the flow for each input `AreaEntity` record
is like this:

    (source area store)
    ↑   |
    |   |-> LOAD one input record
    |   |-> CALL startReduction()
    |   |-> CALL matchWithTempStore()
    |   |-> CALL reduceFromTempStore()
    |   |-> CALL matchWithHouseStore()      <-- new
    |   |-> CALL reduceFromHouseStore()     <-- new
    |   |-> CALL transform()
    |   |-> SAVE TO (dest area store)
    |   |-> REPEAT with the next input record
    |       |
    |_______|

In the flow, `HouseStore`'s match-and-reduce
methods are executed after `TempStore`'s.
However, **that is not always the case**.

## The Order of Matchings

By default, `mjg` does not guarantee the order
of execution of the match-and-reduce methods.

However, you could also specify the order yourself
in the `@MatchWith` annotation. The default order
is 0, so if you want a matching to run sooner - set
its order to -1 or lower. Run later? Set its order
to 1 or greater.

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@MatchWith(
    value = TempStore.class
)
// Execute after TempStore's matching (and any other matchings with order < 1)
@MatchWith(
    value = HouseStore.class,
    order = 1
)
@TransformAndSaveTo(
    value = DestAreaStore.class
)
public class MigrateAreasFromSrcToDest {
    // ...
}
```

This is helpful if some match-and-reduce
methods (e.g. C, D) depend on a value that must
be computed in advance by some other match-and-reduce
methods (e.g. A, B). In that case,
after A and/or B has computed the values,
they must push them into the `aggregates` map
so that C and/or D could use them afterwards.

Last but not least, remember that the
flow for each input record runs in one
thread only, so there is no speed penalty
when you reorder the matchings - the
migration process would run just as
fast.

## Remarks

You could see that `mjg` supports migrating
data between datastores of entirely different
types (SQL vs MongoDB vs any other). That is
the power of abstraction - one common interface
("datastore") and multiple implementations
(SQL, MongoDB, etc.), which makes `mjg`
so extensible.

## See also

- [Home](./README.md)
- [Previous: Handling Duplicates](./003-handling-duplicates.md)
- [Next: Advanced Example - Create Pivot Tables](./005-create-pivot-tables.md)
