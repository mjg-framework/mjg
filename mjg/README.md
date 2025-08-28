# mjg

mjg is a Data Migration Framework for Java
applications.

## Compilation

### Stale Compiler State

If you change the source code of any
annotation processor, be sure to recompile.

**Sometimes,** compiling with IntelliJ IDEA
messes with mjg's annotation processing. So
in case of weird errors, especially test
cases not passing, also try recompiling
**from the command line.**

```sh
mvn clean compile test-compile
```

Alternatively, go to

`File -> Settings -> (Build, Execution, Deployment) -> Compiler`

and uncheck `Build project automatically`.
(This alternative solution is not yet
verified, though.)

The reason for this is IDEs and build tools
(Maven) sometimes enable incremental
build, which will not run mjg's annotation
processor fully and properly, leading
to errors such as missing migrations
in runtime.

### Error from mjg annotation processors

If one of your `@Migration` classes has errors,
mjg annotation processors would print them
out during compilation. Don't be scared of
them! Just read through to fix what went wrong.

For example, following are the messages from
mjg that those `@Migration` classes did not
implement a `handleDuplicate` method.

```plain
Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.14.0:testCompile (default-testCompile) on project mjg-core: Compilation failure: Compilation failure: 
[ERROR] Migration class com.example.mjg.migration_testing.suite1.migrations.M1_PopulatePivotTable_StationIndicators is missing the following methods:
[ERROR]       1. java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity>   handleDuplicate(
[ERROR]           com.example.mjg.migration_testing.suite1.data.entities.StationEntity var1,
[ERROR]           java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity> var2,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationStore var3,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore var4,
[ERROR]           com.example.mjg.storage.DataStoreRegistry var5
[ERROR]       )
[ERROR] Migration class com.example.mjg.migration_testing.suite1.migrations.M2_Migrate_Data_From_StationStore_To_StationStore2 is missing the following methods:
[ERROR]       1. java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationEntity>   handleDuplicate(
[ERROR]           com.example.mjg.migration_testing.suite1.data.entities.StationEntity var1,
[ERROR]           java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationEntity> var2,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationStore var3,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationStore2 var4,
[ERROR]           com.example.mjg.storage.DataStoreRegistry var5
[ERROR]       )
[ERROR] Migration class com.example.mjg.migration_testing.suite1.migrations.M3_Migrate_StationIndicator2 is missing the following methods:
[ERROR]       1. java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2>   handleDuplicate(
[ERROR]           com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity var1,
[ERROR]           java.util.List<com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2> var2,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore var3,
[ERROR]           com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore2 var4,
[ERROR]           com.example.mjg.storage.DataStoreRegistry var5
[ERROR]       )
```

More about this method [in this section](#duplicate-error-handling).

## State Persistence and Error Handling

### State Persistence

The migration program will stop gracefully
upon one of the following conditions:
1. All migrations are run successfully.
2. Some unrecoverable migration errors occur,
   e.g., exceptions in a migration class,
   a violation of cardinality constraints, etc.
3. User presses Ctrl-C.

In extreme cases, e.g., `SIGKILL` received, the
program will stop *forcefully*, not *gracefully*.

When the migration program is stopped *gracefully*,
the `MigrationService` will extract the migration
progress data into a JSON serializable object, so
that the progress could continue the next time if
`MigrationService` is run with that object loaded
in.

**It is your (the user's) responsibility** to persist
it somewhere, says, a JSON file, and load it
into `MigrationService.run()` every time you want
to migrate data, so that previous migration progress
is restored and continued.

### Error Handling

When the migration program is stopped *gracefully*
upon condition 2 or 3 (above), it is crucial to
inspect any failed migration operations in the
JSON content, resolve them, then re-run
`MigrationService.run()` with the altered
JSON content.

To do this, let's see the schema of this JSON object,
conveniently written as a TypeScript type declaration
below.

```typescript
type MigrationProgress_ObjectSchema = {
    "metadata": {
        "timestamp": string /*e.g. "2025-08-29T00:33:51.600238326"*/,
        "completedMigrationFQCNs": Array<string>, /*array of migration FQCNs*/
        "inProgressMigrationFQCNs": Array<string>, /*array of migration FQCNs*/
    },
    
    "migrationProgress": {
        [fqcn: string]: {
            "fqcn": string,
            "migratedRecords": Array<string | number /* int, long */>,
            "failedRecords": Array<{
                "id": string | number /* int, long */,
                "description": string,
                "cause": string,
                "action": "IGNORE"
                    | "RETRY"
                    | "TAKE(0)" | "TAKE(1)" | "TAKE(N-2)" | "TAKE(N-1)" /*
                        | ... TAKE(x) or TAKE(N - x) with x being a natural number
                    */,
                "timestamp": string,
            }>,
        },
    },
    
    "fataLErrors": Array<string>,
}
```

Each migration class is identified by its FQCN
(Java Fully Qualified Class Name).

For resolving errors, focus on the `failedRecords`
field of each migration class under `migrationProgress`,
which lists all the failed records during the
process of running that particular migration
class.

For each such record, you could review
its ID, description and the cause of the
error, along with other data and
metadata.

Then, you specify an action for
the `MigrationService` to take next time
upon this particular record: set `action`
to either `IGNORE` or `RETRY`. The default
action is `RETRY`.

If you set the action to `IGNORE`, the
record will still be ignored next time, i.e.,
not migrated. Only if you set it to `RETRY`
will the `MigrationService` give it another try
next time. In fact, most of the time, it might
just be due to some exception thrown in your
`@Migration` classes' methods. If that is the
case, fix your code, leave the action to
be `RETRY`, and simply rerun the migration
with the altered migration progress JSON
data.

In case the cause is a cardinality violation
when the required cardinality is `EXACTLY_ONE`
or `ZERO_OR_ONE`, but there are more than one
matched/transformed records, you could also
specify the action to be one of those
`TAKE(...)` actions, which picks one
record to satisfy the cardinality requirement.

Meanwhile, if `ONE_OR_MORE` cardinality
requirement is violated, that means there are
absolutely zero matched/transformed records,
so there is no way you could "take" any record
to compromise, so those `TAKE(...)` actions
do not apply here and, if specified, will be
considered invalid and another error will be
thrown.

`ZERO_OR_MORE` cardinality never fails for
any number of matched/transformed records, so
`TAKE(...)` actions do not apply either.

## Duplicate Error Handling

This is a special type of error that emerges
when a `DuplicateDataException` is thrown
in the transform phase.

In the vast majority of cases, the number
of records being duplicate of each other (and
violating a unique constraint) is just... one.
Indeed, suppose you are inserting a new `User` and
find out that another `User` with the same
email exists. See? There are only two duplicate
records violating the constraint of unique
email addresses.

In contrast, the whole `matchWith...`,
`reduceFrom...` and `transform` chain often
deals with large numbers of records, so it has
pagination automatically handled by the framework
(which is the reason why you're not supposed
to touch the stores directly there - I built
the chain to ease the pain!).

So the idea is that in case of a duplicate error,
the number of records we need to query from datastore
to remedy the error is usually so small that we
don't need pagination-based solutions for
managing this operation. Instead, you are allowed
to interact with the data stores directly.
You are to implement a duplicate resolution method
like this in your `@Migration` class (suppose
the input record type is `A`, output record type
is `B`):

```java
public List<B> handleDuplicate(
    A inputRecord,
    List<B> outputRecords, // records you returned from .transform()
    AStore inputDataStore,
    BStore outputDataStore,
    DataStoreRegistry dataStoreRegistry
) {
    // ...
}
```

This method shall return the "remedied"
version of `outputRecords` so that
the framework could try saving them
instead to *hopefully avoid another*
*duplicate error* (more on this at
the end of this section).

Especially, if you want to merge the
`outputRecords` with the existing
duplicate ones, you could query
the existing duplicates from the
`outputDataStore` by yourself
to get the IDs for the resulting
records, like the `!!IMPORTANT!!`
lines in the following example:

```java
//import java.time.LocalDateTime;
//import java.util.function.Function;
//import java.util.stream.Collectors;

/**
 * Here we want to merge the information
 * of existing users with the new users.
 */
public List<User> handleDuplicate(
        Account inputRecord,
        List<User> outputRecords,
        AccountStore inputDataStore,
        UserStore outputDataStore,
        DataStoreRegistry dataStoreRegistry
) {
   Set<String> existingEmails = outputRecords.stream()
           .map(User::getEmail)
           .collect(Collectors.toSet());
   List<User> existingUsers = outputDataStore
           .getAllByEmailIn(existingEmails);
   Map<String, User> newUserByEmail = outputRecords
           .stream()
           .collect(Collectors.toMap(
                   User::getEmail,
                   Function.identity(),
                   (existing, replacement) -> existing,
                   HashMap::new
           ));

   return existingUsers.stream().map(existingUser -> {
      User newUser = newUserByEmail.get(existingUser.getEmail());
      // !!!!!!!!!!!!!!! IMPORTANT !!!!!!!!!!!!!!!
      // Match by ID to cause an update instead of inserting new
      newUser.setId(existingUser.getId());
      
      // For duplicate records, keep some fields intact...
      newUser.setName(existingUser.getName());
      newUser.setPhoneNumber(existingUser.getPhoneNumber());
      
      // ... while changing others
      newUser.setLastLoginTime(LocalDateTime.now());
      
      return newUser;
   }).toList();
}
```

If you choose NOT to handle the
duplicate error, return `null`,
in which case the error will be
handled according to the current
`@ErrorResolution` setting, just
like any other migration error.

It is OK to throw an exception
in this method when resolution
goes wrong.

Besides the specified input and output data
stores, here is how to access *any* other
store, for example `CStore`:

```java
DataStore<?, ?, ?> instanceOfCStore = dataStoreRegistry.get(
    CStore.class.getCanonicalName()
);
```

Why give access to just any store? Since I believe
duplicate resolution could be very complicated
at times...

Yes, for example, **what if `handleDuplicates`**
**returns records that, when saved, also yield**
**`DuplicateDataException`?** If not handled properly,
this could result in a devastating infinite
recursion. The solution to this is, well,
`handleDuplicates` is only allowed to run
once! If it fails, this particular input record
is marked as a failed record, and will be
sent to a retry queue, where the whole migration
pipeline (`matchWith...`, `reduceFrom...`,
`transform` and possibly this `handleDuplicates`
again) will be executed for this input
record for some more times. The number
of retry times and the delay between
them are configured in `@ErrorResolution`.
If the input record still fails to migrate
after all this, it will be reported in the
`MigrationProgress` JSON object, and, depending
on the error resolution settings, the migration
process could continue or stop.

## Cautions on Writing Migration Code

### Subclassing DataStore

All implemented `DataStore` methods must be
thread-safe.

Implementations of `DataStore::saveAll()` are
assumed to have **transaction-like behavior**,
i.e., either all given records are successfully
saved, or none at all - in case of error.
That way, when there is error, the framework
could retry the records one by one to figure
out the true culprit, without re-saving any
record in the batch, which might result in data
integrity violations (e.g., duplicate values
when/where unique constraints are in effect).

### Transformation

Inside `transform()`, in case the entity class of
the input datastore is the same as that of the
output datastore, you must NOT return the same
record object.

For example:

```java
// Wrong
public List<StationEntity> transform(
    Map<String, Object> aggregates,
    StationEntity station
) {
    return List.of(station);
}
```

You MUST always create and return a new record
object, even if you just want to copy the data
verbatim, like this:

```java
// Correct
public List<StationEntity> transform(
    Map<String, Object> aggregates,
    StationEntity station
) {
    StationEntity newStation = new StationEntity(
        station.getName(),
        station.getType()
        // ...
    );
    return List.of(newStation);
}
```

## Future Improvements

### Robustness

When any record in a migration fails
to migrate, mjg would finish the
appropriate migration and stop the migration
process altogether.

This could be improved in the future. For
example, suppose we have a migration
dependency graph like this:

```plain
A -> B \
        \
         --> E
        /
C -> D /

|
V

F -> G
```

and one record in migration B fails to migrate,
so mjg will migrate other records in B, then
stop the migration process entirely. However,
before stopping, we could execute migrations
C, D, F and G. Indeed, since they do not
depend on B whatsoever, they could be executed
independently. At this time, we are not able
to do that just yet.

The key to solving this problem might be to
scan all migrations that do not directly
depend on B. In the graph above, those
migrations would indeed be C, D, F, and G.
(while A qualifies, it doesn't count since it
was already completed - a prerequisite for B
to execute.)

Ultimately, we want to finish as many migrations
as possible, so that more work is done, and
more errors are spotted earlier also.
