# Advanced Example - Matching And Averaging

- [Advanced Example - Matching And Averaging](#advanced-example---matching-and-averaging)
  - [Goal and Setup](#goal-and-setup)
  - [Matching](#matching)
  - [The Full Migration Process of mjg](#the-full-migration-process-of-mjg)
  - [Back to Our Example](#back-to-our-example)
  - [Notes on Batching of the Reduction Step](#notes-on-batching-of-the-reduction-step)
  - [Optimization: Reducing Memory Usage](#optimization-reducing-memory-usage)
  - [Final Migration Code](#final-migration-code)
  - [Don't forget to wire the datastore into your migrate service](#dont-forget-to-wire-the-datastore-into-your-migrate-service)
  - [See also](#see-also)

## Goal and Setup

Say, for each `AreaEntity` you have to re-calculate
the average temperature in that area, and that for
each area, we have several temperature measurement
results for it, stored in another repository.

So, let's write the entity.

```java
@Getter
@Setter
@Document(value = "temps")
public class TempEntity implements MigratableEntity {
    @Override
    public Serializable getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "Temp(id=" + id + ")";
    }

    @Id
    public String id;

    @Field(value = "area_code")
    public String areaCode;

    @Field(value = "value")
    public Double value;
}
```

Follow the same procedures
[as in the previous article](./001-simple-example.md)
to create the repository and store for this,
named `TempRepository` and `TempStore`.

```java
public interface TempRepository extends MigratableMongoRepository<TempEntity, String> {
    /**
     * We will need this method later.
     * Could be used in the filter step,
     * since it accepts Pageable at the end and returns a Page<T>.
     */
    Page<TempEntity> findAllByAreaCodeIn(
        Collection<String> areaCode,
        Pageable pageable
    );
}
```

```java
@Component
@AllArgsConstructor
public class TempStore
extends MongoRepositoryStore<TempEntity, String>
{
    @Getter
    private final TempRepository repository;
}
```

So now the intent is:  
**For each `oldArea` from `srcAreaStore`,**  
**create `newArea` with same content as `oldArea` except id,**  
**find all `temp` records from `TempStore` where `temp.areaCode = oldArea.areaCode`,**  
**calculate the average value of `temp.value` of all those records,**  
**assign it to `newArea.averageTemperature`,**  
**then save `newArea` to `destAreaStore`,**  
**handle any error in the process**.

## Matching

We just have to modify our migration class
to add a new `@MatchWith` directive. You
can see that now we have to match each
input `AreaEntity` record to 0, 1, or
more `TempEntity` records, hence the name
"match with".

Our annotations now:

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@MatchWith(
    value = TempStore.class
)
@TransformAndSaveTo(
    value = DestAreaStore.class
)
public class MigrateAreasFromSrcToDest {
    // ...
}
```

You can apply several `@MatchWith` annotations
to one migration class. For each `@MatchWith`,
we need to define two more methods in our
migration class, named `matchWithXXX` and
`reduceFromXXX`, where `XXX` is the store
class name.

Why those two? Let's see the full migration
process.

## The Full Migration Process of mjg

Here is the full process of `mjg` running
one *migration class*. The case of several
migration classes will be covered in
[a later article](./006-chaining-migrations.md).

Note that while steps 1, 3, 4, 5 are unchanged
compared to the simplified migration process we've
seen earlier, step 2 incorporates matching logic,
which we'll use in our example.

1. The migration service pulls records from the source store
    denoted by `@ForEachRecordFrom`.

2. For each such input record, it:

    - Creates a new empty map of type `Map<String, Object>`.
        We call that the `aggregates` map.
    
    - Calls `startReduction` with the `aggregates` and
        the input record as arguments.
    
    - Then, for each matching store denoted by a `@MatchWith`,
        e.g. `@MatchWith(XXX.class)`:
        
        - The framework calls `matchWithXXX`, where `XXX`
            is the store name, to get a *filter set*.
        - It then loads records from store `XXX` matching
            that *filter set* - not in full, but batch by batch
            (i.e. paginated).
        - For each batch (page) of data, it calls `reduceFromXXX`
            with the `aggregates` map and that batch as arguments.
        - This repeats for each of the next `@MatchWith` annotations,
            and the same `aggregates` map is passed around.
        - (If `matchWithXXX` returned null, there would be no
            matching records, the framework would't read anything
            from the matching store `XXX`, and `reduceFromXXX` would
            not be called either.)
    
    - When all matching is done, the framework calls
        `transform` with the input record and the `aggregates`
        map as arguments, to get the output record(s).

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

Note that the data duplicate exception will
be discussed in detail [in the next article](./003-handling-duplicates.md).
You might ask why use `aggregates`? We will use it
in the next sections!

## Back to Our Example

For matching an area with the temps,
we need to define:

- **Step 1. Matching:** How to determine which temps correspond
    to a given input `AreaEntity` record.
- **Step 2. Reduction:** What to do with those temps.

Normally, we could do it like this (this
is not the best approach, we will revise it
later):

```java
// Step 1: Matching
String areaCode = inputAreaRecord.getAreaCode();
List<TempEntity> allTemps;
PageRequest pageable = PageRequest.of(PAGE_SIZE, 0); // 0 is index of the first page
while (true) {
    List<TempEntity> tempBatch = tempRepository.findAllByAreaCode(areaCode, (Pageable) pageable);
    if (tempBatch.isEmpty()) break;
    // Step 2: Reduction
    allTemps.addAll(tempBatch);
    // next page
    int nextPage = pageable.getPageNumber() + 1;
    pageable = PageRequest.of(PAGE_SIZE, nextPage);
}

// Step 3: Transform and save
double avg = computeAverage(allTemps) // some function to calculate average
outputAreaRecord.setAverageTemperature(avg);
destAreaRepository.save(outputAreaRecord);
```

How to do that with `mjg`?

**In step 1**, we need to define a `matchWithXXX`
method, that returns a *filter set* which
will only pull from `TempStore` the
temp records that has the same `areaCode`.
Notice that the store class name is `TempStore`,
or:

    XXX = TempStore

so the method must be named `matchWithTempStore`,
like this:

```java
// import com.example.mjg.spring.filtering.SpringRepositoryFilterSet;

public SpringRepositoryFilterSet<TempEntity, String>
// String is the type of ID of TempEntity
matchWithTempStore(
    AreaEntity inputAreaRecord,
    Map<String, Object> aggregates,     // unused
    TempStore tempStore                 // unused
) {
    String areaCode = inputAreaRecord.getAreaCode();
    if (areaCode == null || areaCode.isEmpty()) {
        // No area code so can't match to any TempEntity record
        return null;
    }

    return SpringRepositoryFilterSet.of(
        TempRepository::findAllByAreaCodeIn,
        Set.of(areaCode)
    );
}
```

That is, for each `inputAreaRecord`, we extract
its `areaCode`, then look up all temps that
have the same `areaCode`.

Why `Set.of(areaCode)`?
Because earlier, we defined in `TempRepository` the method
`findAllByAreaCodeIn` to accept an argument of
type `Collection<String>`, which is assignable
from a `Set<String>`.

You don't pass a `Pageable` here;
it's the framework's job to do that with its
automatic batching feature.

**In step 2**, now that we've found the matching
temps, one way to calculate the average is to store
all these temps into `aggregates` and compute them later
in `transform`.

```java
public void reduceFromTempStore(
    Map<String, Object> aggregates,
    List<TempEntity> tempRecordsBatch
) {
    List<TempEntity> allMatchingTempRecords = aggregates.computeIfAbsent(
        "temps",
        k -> new ArrayList<TempEntity>()
    );
    allMatchingTempRecords.addAll(tempRecordsBatch);
}
```

Note the method is named `reduceFromXXX`
where `XXX = TempStore`.

Why we need to store them into `aggregates`? Let's see
our new `transform` method:

```java
public List<AreaEntity> transform(
    Map<String, Object> aggregates,
    AreaEntity oldArea
) {
    AreaEntity newArea = new AreaEntity();
    newArea.setAreaCode(oldArea.getAreaCode());
    newArea.setAreaName(oldArea.getAreaName());
    newArea.setDescription(oldArea.getDescription());
    newArea.setOrderNo(oldArea.getOrderNo());
    // Don't take outdated value like before!
    //      newArea.setAverageTemperature(oldArea.getAverageTemperature());
    // Now we compute it
    List<TempEntity> allMatchingTempRecords = aggregates.getOrDefault(
        "temps",
        k -> new ArrayList<TempEntity>()
    );
    newArea.setAverageTemperature(
        computeAverageTemperature(allMatchingTempRecords)
    );
    return List.of(newArea);
}

public static double computeAverageTemperature(
    List<TempEntity> temps
) {
    if (temps.isEmpty()) return 0.0;
    double sum = 0.0;
    int N = temps.getSize();
    for (TempEntity t : temps) {
        sum += Optional.ofNullable(t.getValue()).orElse(0.0);
    }
    return sum / N;
}
```

So, if we don't save the matching temps into
`aggregates`, there is no way for `transform`
to compute the average value out of them!

Just leave `startReduction` and
`handleDuplicate` methods to be the same
[as in the previous article](./001-simple-example.md),
and now our migration class is complete! It
does exactly what we expected it to do
earlier:

**For each `oldArea` from `srcAreaStore`,**  
**create `newArea` with same content as `oldArea` except id,**  
**find all `temp` records from `TempStore` where `temp.areaCode = oldArea.areaCode`,**  
**calculate the average value of `temp.value` of all those records,**  
**assign it to `newArea.averageTemperature`,**  
**then save `newArea` to `destAreaStore`,**  
**handle any error in the process**.

## Notes on Batching of the Reduction Step

Why write like this:

```java
// correct approach as we've done
public void reduceFromTempStore(
    Map<String, Object> aggregates,
    List<TempEntity> tempRecordsBatch
) {
    List<TempEntity> allMatchingTempRecords = aggregates.computeIfAbsent(
        "temps",
        k -> new ArrayList<TempEntity>()
    );
    allMatchingTempRecords.addAll(tempRecordsBatch);
}
```

instead of storing the batch directly like this?

```java
// wrong approach
public void reduceFromTempStore(
    Map<String, Object> aggregates,
    List<TempEntity> tempRecordsBatch
) {
    aggregates.put("temps", tempRecordsBatch);
}
```

Well, that's wrong, because `reduceFromTempStore`
is called for each batch of temps. Suppose the
batch size is 512, and we have 1025 temps matching
the current `AreaEntity` record. So we would need
to call `reduceFromTempStore` 3 times, each receiving
a batch of 512, 512 and 1 record(s) of temps,
respectively. Had we followed the wrong
approach, a call with new batch will essentially
override the old one, so in `transform` we would
only see 1 temp record from the last batch!

Remember, we need to *accumulate* the results
inside a `reduceFromXXX` method.

## Optimization: Reducing Memory Usage

The default batch size is 512, which means, if
there are at least 512 areas, each matching with
at least 512 temp records, and all those areas
are processed in parallel, we end up having

    512 (areas)
    + 512 (temps)
    + 512 x 512 (combination)
    = 263168 (total)

records in RAM at the same time. While this is
not a big deal in modern computers, there is
room for improvement.

The simplest solution to reduce memory usage
now, is to lower the batch size.

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class,
    batchSize = 512     // load at most 512 input area records at once
)
@MatchWith(
    value = TempStore.class,
    batchSize = 8       // process at most 8 temps at once per each area
)
@TransformAndSaveTo(
    value = DestAreaStore.class,
    batchSize = 512     // save at most 512 output area records to the new store at once
)
public class MigrateAreasFromSrcToDest {
    // ...
}
```

However, since we are just calculating
the average, we could do that with each
batch, and only store the numbers.

Our idea previously:

```java
// Step 1: Matching
String areaCode = inputAreaRecord.getAreaCode();
List<TempEntity> allTemps;
PageRequest pageable = PageRequest.of(PAGE_SIZE, 0); // 0 is index of the first page
while (true) {
    List<TempEntity> tempBatch = tempRepository.findAllByAreaCode(areaCode, (Pageable) pageable);
    if (tempBatch.isEmpty()) break;
    // Step 2: Reduction
    allTemps.addAll(tempBatch);
    // next page
    int nextPage = pageable.getPageNumber() + 1;
    pageable = PageRequest.of(PAGE_SIZE, nextPage);
}

// Step 3: Transform and save
double avg = computeAverage(allTemps) // some function to calculate average
outputAreaRecord.setAverageTemperature(avg);
destAreaRepository.save(outputAreaRecord);
```

Of course, it is very inefficient.

**Now, we have a new step (step 0) added and**
**only step 2 and 3 are modified.**

```java
// Step 0: Start Reduction (NEW STEP)
double sum = 0.0;
long count = 0;

// Step 1: Matching
String areaCode = inputAreaRecord.getAreaCode();
PageRequest pageable = PageRequest.of(PAGE_SIZE, 0); // 0 is index of the first page
while (true) {
    List<TempEntity> tempBatch = tempRepository.findAllByAreaCode(areaCode, (Pageable) pageable);
    if (tempBatch.isEmpty()) break;

    // Step 2: Reduction (MODIFIED)
    sum += computeSum(tempBatch); // some function to calculate sum
    count += tempBatch.size();

    // next page
    int nextPage = pageable.getPageNumber() + 1;
    pageable = PageRequest.of(PAGE_SIZE, nextPage);
}

// Step 3: Transform and save (MODIFIED)
double avg;
if (n == 0) {
    avg = sum / n;
} else {
    avg = 0.0;
}
outputAreaRecord.setAverageTemperature(avg);
destAreaRepository.save(outputAreaRecord);
```

In `mjg` world, that step 0 is accomplished in
the method `startReduction` - you might have guessed
it! It serves as the initialization for the whole
matching - reduction phase. **If there are several**
**matchings, `startReduction()` is still called**
**once per record:**

    startReduction()
        -> matchWithA() -> reduceFromA()
        -> matchWithB() -> reduceFromB()
        -> ...

Now we need to rewrite `startReduction`,
`reduceFrom` and `transform` according to the
new idea. The full code is shown in the
following section.

## Final Migration Code

The flow is:

    (source area store)
    ↑   |
    |   |-> LOAD one input record
    |   |-> CALL startReduction()
    |   |-> CALL matchWithTempStore()
    |   |-> CALL reduceFromTempStore()
    |   |-> CALL transform()
    |   |-> SAVE TO (dest area store)
    |   |-> REPEAT with the next input record
    |       |
    |_______|

Of course, with batching, several
input records will be processed
simultaneously with that flow
at the same time.

The flow for each input record
will be run in one single
thread, so no need to worry
about synchronization, race
conditions etc. when
writing those methods.

Putting it together:

```java
@Migration
@ForEachRecordFrom(
    value = SrcAreaStore.class
)
@MatchWith(
    value = TempStore.class
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
    }

    /* the matching method is unchanged */
    public SpringRepositoryFilterSet<TempEntity, String>
    matchWithTempStore(
        AreaEntity inputAreaRecord,
        Map<String, Object> aggregates,     // unused
        TempStore tempStore                 // unused
    ) {
        String areaCode = inputAreaRecord.getAreaCode();
        if (areaCode == null || areaCode.isEmpty()) {
            // No area code so can't match to any TempEntity record
            return null;
        }

        return SpringRepositoryFilterSet.of(
            TempRepository::findAllByAreaCodeIn,
            Set.of(areaCode)
        );
    }

    /* MODIFIED */
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

        AreaEntity newArea = new AreaEntity();
        newArea.setAreaCode(oldArea.getAreaCode());
        newArea.setAreaName(oldArea.getAreaName());
        newArea.setDescription(oldArea.getDescription());
        newArea.setOrderNo(oldArea.getOrderNo());
        // newArea.setAverageTemperature(oldArea.getAverageTemperature());
        newArea.setAverageTemperature(avg);
        return List.of(newArea);
    }

    /* unchanged, we still don't handle duplicates */
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

## Don't forget to wire the datastore into your migrate service

Since we've just created a new store (`TempStore`),
we need to add it in our migration service that
we already defined [in the previous article](./001-simple-example.md#5-run-mjg):

```java
@Slf4j
@Service
public class MyMigrateService {
    // ... unchanged ...

    public MyMigrateService(
        SrcAreaStore srcAreaStore,
        DestAreaStore destAreaStore,
        TempStore tempStore,
    ) {
        dataStoreRegistry = new DataStoreRegistry();

        dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
        dataStoreRegistry.set(DestAreaStore.class, destAreaStore);
        dataStoreRegistry.set(TempStore.class, tempStore);

        migrationService = new MigrationService(dataStoreRegistry);

        // ... unchanged ...
    }

    // ... unchanged ...
}
```

Now we're ready to run our migration application
with our modified migration class!

## See also

- [Home](./README.md)
- [Previous: Simple Example](./001-simple-example.md)
- [Next: Handling Duplicates](./003-handling-duplicates.md)
