# Advanced Example - Create Pivot Tables

- [Advanced Example - Create Pivot Tables](#advanced-example---create-pivot-tables)
  - [Goal](#goal)
  - [Set up](#set-up)
    - [PersonEntity](#personentity)
    - [HousePersonEntity](#housepersonentity)
    - [Register the new stores](#register-the-new-stores)
  - [Our Migration Class](#our-migration-class)
    - [It is a new migration class](#it-is-a-new-migration-class)
    - [Think about it the mjg way](#think-about-it-the-mjg-way)
    - [The Migration Class](#the-migration-class)
    - [Why That is Better](#why-that-is-better)
    - [That is Better, but in THAT case only](#that-is-better-but-in-that-case-only)
  - [See also](#see-also)

## Goal

One house may have several people living in it,
while one person can also reside in several houses.
There exists an N-N relationship between
persons and houses. Typically, such a
relationship is represented by a pivot
table, like:

```sql
/* SQL */
id BIGINT PRIMARY KEY,
house_id BIGINT,
person_id BIGINT
```

We already have `HouseEntity`. We are going
to create a `PersonEntity`. Suppose the pivot
entity is `HousePersonEntity` - we are going to
create it too.

We will use `mjg` to generate `HousePersonEntity`
data for that pivot table in an automated
fashion.

## Set up

You could skip this part if you understood
the procedure.

`HouseEntity` has already been set up in a
[previous article](./004-multiple-matchings.md).

### PersonEntity

1. The entity:

    ```java
    @Getter
    @Setter
    @Entity
    public class PersonEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;
    }
    ```

2. The Spring JPA repository:

    ```java
    import com.example.mjg.spring.repositories.MigratableSpringRepository;

    public interface PersonRepository extends MigratableSpringRepository<PersonEntity, Long> {}
    ```

3. The datastore:

    ```java
    import com.example.mjg.spring.stores.SpringRepositoryStore;
    import lombok.Getter;
    import lombok.AllArgsConstructor;
    import org.springframework.stereotype.Component;

    @Component
    @AllArgsConstructor
    public class PersonStore
    extends SpringRepositoryStore<PersonEntity, Long>
    {
        @Getter
        private final PersonRepository repository;
    }
    ```

### HousePersonEntity

1. The entity:

    ```java
    @Getter
    @Setter
    @Entity
    public class HousePersonEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        // You could create foreign key if you wish to
        private Long houseId;
        private Long personId;
    }
    ```

2. The Spring JPA repository:

    ```java
    import com.example.mjg.spring.repositories.MigratableSpringRepository;

    public interface HousePersonRepository extends MigratableSpringRepository<HousePersonEntity, Long> {}
    ```

3. The datastore:

    ```java
    import com.example.mjg.spring.stores.SpringRepositoryStore;
    import lombok.Getter;
    import lombok.AllArgsConstructor;
    import org.springframework.stereotype.Component;

    @Component
    @AllArgsConstructor
    public class HousePersonStore
    extends SpringRepositoryStore<HousePersonEntity, Long>
    {
        @Getter
        private final HousePersonRepository repository;
    }
    ```

### Register the new stores

Register the new stores in our migration service:

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
        PersonStore personStore,           // <-- new store
        HousePersonStore housePersonStore  // <-- new store
    ) {
        dataStoreRegistry = new DataStoreRegistry();
        // ...
        // new stores
        dataStoreRegistry.set(PersonStore.class, personStore);
        dataStoreRegistry.set(HousePersonStore.class, housePersonStore);

        migrationService = new MigrationService(dataStoreRegistry);

        // ... unchanged ...
    }

    // ... unchanged ...
}
```

## Our Migration Class

### It is a new migration class

We are "migrating" data from 2 stores
`HouseStore`, `PersonStore`
to `HousePersonStore`.

It's not migrating from `SrcAreaStore`
to `DestAreaStore` like we used to do
in the previous articles, *so we create*
*a new migration class* instead of
writing more code in the old class!

### Think about it the mjg way

`@ForEachRecordFrom` only allows to load
records from one datastore, not two!

However, notice that each house may
*correspond* to zero or more people.
Likewise, each person may also
*correspond* to zero or more houses.

*To correspond to* is the keyword here,
it means the same thing as *to match with*.
So we could re-state it like this:

1. Each house may *match with* zero
    or more people.
2. Each person may also *match with* zero
    or more houses.

At this point you might have guessed that
we could use `@MatchWith` for this, right?

Yes. Actually there are two approaches:

1. Each house may *match with* zero
    or more people.
    
    ```java
    @ForEachRecordFrom(HouseStore.class)
    @MatchWith(PersonStore.class)
    @TransformTo(HousePersonStore.class)
    // ...
    ```

2. Each person may *match with* zero
    or more houses.

    ```java
    @ForEachRecordFrom(PersonStore.class)
    @MatchWith(HouseStore.class)
    @TransformTo(HousePersonStore.class)
    // ...
    ```

Which approach to choose? Well, let's choose
the second one, write the full migration code
for it, and we will see how that is actually
better *in this case*.

### The Migration Class

For simplicity, we generate all pairs of
house-store possible from existing `HouseStore`'s
and `PersonStore`'s data. You could tweak it
to match only the houses that the current person
lives in.

(You might ask if we know *which houses a person*
*lives in* already, why generate a pivot table? Well,
that could facilitate lookup of *which persons*
*live in a house*.)

```java
@Migration
@ForEachRecordFrom(
    value = PersonStore.class,
    batchSize = 128              // explained later
)
@MatchWith(
    value = HouseStore.class
)
@TransformAndSaveTo(
    value = HousePersonStore.class
)
public class PopulatePivotTableHousesPersons {
    public void startReduction(
        PersonEntity inputRecord,
        Map<String, Object> aggregates
    ) {
        aggregates.put("personId", inputRecord.getId());
        aggregates.put("houseIdSet", new HashSet<Long>());
    }

    public SpringRepositoryFilterSet<TempEntity, String>
    matchWithHouseStore(
        PersonEntity inputRecord,
        Map<String, Object> aggregates,
        HouseStore houseStore               // unused
    ) {
        return SpringRepositoryFilterSet.of(
            HouseRepository::findAll
        );
        // You could also write this instead, it's shorter
        // return SpringRepositoryFilterSet.findAll();
    }

    public void reduceFromHouseStore(
        Map<String, Object> aggregates,
        List<HouseEntity> houseRecordsBatch
    ) {
        Set<Long> houseIdSet = (Set<Long>) aggregates.get("houseIdSet");

        for (HouseEntity house : houseRecordsBatch) {
            houseIdSet.add(house.getId());
        }
        // No need for this since we only mutated the set
        // aggregates.add("houseIdSet", houseIdSet);
    }

    public List<HousePersonEntity> transform(
        Map<String, Object> aggregates,
        PersonEntity inputRecord
    ) {
        Long personId = (Long) aggregates.get("personId");
        Set<Long> houseIdSet = (Set<Long>) aggregates.get("houseIdSet");
        List<HousePersonEntity> newRecords = new ArrayList<>();
        for (Long houseId : houseIdSet) {
            HousePersonEntity r = new HousePersonEntity();
            r.setPersonId(personId);
            r.setHouseId(houseId);
            newRecords.add(r);
        }
        return newRecords;
    }

    public List<HousePersonEntity> handleDuplicate(
        DuplicateDataException exception,
        HousePersonEntity inputRecord,
        List<HousePersonEntity> outputRecords,
        PersonStore srcStore,
        HousePersonStore destStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        return null;
    }
}
```

The code basically pushes all house IDs it received
into a set in `aggregates`, and at transform
phase it converts those house IDs, along with
the current person ID, to several `HousePersonEntity`
records that could be saved to the pivot table.

### Why That is Better

And why is there `batchSize = 128`?

In processing one input record, let `T`
be the number of output records returned
by `transform()`. We want to minimize `T`.

It's not because we worry
about too many records busted into the
database at once (since we've got
automatic batching, remember?).

It's because we still need to watch out
for **out-of-memory** issue: a large value of
`T` means the list of `HousePersonEntity`
instances are containing too many elements,
which could eat up RAM. That's the real
reason why we would want to minimize `T`,
or more exactly, minimize the maximum
value of `T`, or `max(T)`.

We set a lower batchSize
(128 instead of 512 by default) to
also reduce the chance of running out
of memory. Now we want to find out
which of the two approaches minimize
`max(T)`.

First, let's revisit the first approach:
Each house may *match with* zero
or more people.

```java
@ForEachRecordFrom(HouseStore.class)
@MatchWith(PersonStore.class)
@TransformTo(HousePersonStore.class)
// ...
```

Suppose a house in the dataset
has at most `P` people living in
it. So

    max(T)[approach 1] = P

Now back to our chosen approach:
Each person may *match with* zero
or more houses.

```java
@ForEachRecordFrom(PersonStore.class)
@MatchWith(HouseStore.class)
@TransformTo(HousePersonStore.class)
// ...
```

Suppose a person in the dataset
lives in at most `H` houses. So

    max(T)[approach 2] = H

We can realize that

    H < P

since in this economy, people would
rather remain with the family, live
with their parents and cousins
(i.e. increase `P`) instead of
moving out, buying their own houses
(i.e. increase `H`).

(Or at least I have not known many
rich people out there!)

Thus we have

    max(T)[approach 2]    <    max(T)[approach 1]

Meaning, the second approach tends
to have lower number of output records
at one time, compared to the first
approach. Which means, the second
approach is better. This is our
conclusion.

### That is Better, but in THAT case only

Our conclusion is rather subjective (from
a poor's point of view lol) and it is also
very specific to this case
(houses vs persons living in them),
so this does not apply to almost
any other cases.

The moral is, we should always watch out
for memory issues like this, and when
there are several approaches to choose
from (especially when generating data
for a pivot table), always look for
the best one!

## See also

- [Home](./README.md)
- [Previous: Advanced Example - Multiple Matchings](./004-multiple-matchings.md)
- [Next: Advanced Example - Chaining Migrations](./006-chaining-migrations.md)
