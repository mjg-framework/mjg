# Migration Progress JSON Format

- [Migration Progress JSON Format](#migration-progress-json-format)
  - [Even more Fault Tolerance!](#even-more-fault-tolerance)
  - [Migration Progress JSON Serialization](#migration-progress-json-serialization)
  - [Progress Persistence](#progress-persistence)
  - [Migration Progress JSON Format](#migration-progress-json-format-1)
  - [Back to our Original Question](#back-to-our-original-question)
  - [See also](#see-also)

## Even more Fault Tolerance!

[In a previous article](./007-fault-tolerance.md),
we've discussed fault tolerance in the form
of retrying stuff at the point of failure.
If, after a specified number of retries,
an input record still fails to migrate,
then it will be reported to the current
*migration progress*.

So, what about some failed records that could
be **retried later**, even *after* the program
has exited ?

If you load and save the *migration progress*
properly, [as described in a previous article](./001-simple-example.md#5-run-mjg),
you have the chance to retry those failed records!

The *migration progress* is actually a
`MigrationProgress` instance that `mjg`
maintains internally. All successful
records, failed records, successful
migrations, ongoing migrations will
be reported to and contained in that
instance.

So first, we need to understand how to
save and load it properly.

## Migration Progress JSON Serialization

Suppose you want to load/save a
`MigrationProgress` instance from/to
a JSON file.

It in fact is the simplest method to
keep the migration progress.

[As described here](./000-overview.md#progress-persistence),
if you want to JSON-serialize/deserialize a
`MigrationProgress` object, always use the
ObjectMapper obtained by `ObjectMapperFactory.get()`:

```java
import com.example.mjg.utils.ObjectMapperFactory;

ObjectMapper objectMapper = ObjectMapperFactory.get();

String jsonString = objectMapper
    .writeValueAsString(migrationProgress);
```

instead of what you would normally do with Jackson:

```java
ObjectMapper objectMapper = new ObjectMapper();

String jsonString = objectMapper
    .writeValueAsString(migrationProgress);
```

The reason for this is the `ObjectMapper` instance
provided via `mjg`'s method `ObjectMapperFactory.get()`
would support (de)serializing values of some
special data types that are not Jackson-serializable
by default.

If you don't use it, chances are you would fail
to serialize it, or you cannot restore the saved
*migration progress* properly.

Now that you understand how to (de)serialize
a `MigrationProgress` instance, it's easy
to write code to **load** it from a file, then
pass it to `migrationService.run()`, as
[demonstrated in this article](./001-simple-example.md#5-run-mjg).

That article also shows how to **save**
the migration progress. We will walk through
it in details in the next section.

## Progress Persistence

You need to register a callback for `mjg` to pass
the current `MigrationProgress` instance, and that
callback shall save it somewhere so that it could
be loaded the next time you call `migrationService.run()`.

That is called a **progress persistence callback**.
You could register *multiple* such callbacks.

An important point is, this callback will be
called at critical moments to prevent data
loss, especially when the migration process
(i.e. the process happens when you call
`migrationService.run()`) is stopped gracefully.

The migration process stops gracefully
upon one of the following conditions:

1. All migrations are run successfully.
2. Some unrecoverable migration errors occur.
3. User presses `Ctrl-C` or sends `SIGTERM`,
    `SIGINT` (on Linux), `CTRL_C_EVENT` (on Windows),
    or any other events that trigger
    a shutdown hook added using Java method
    `Runtime.getRuntime().addShutdownHook()`.

In extreme cases, e.g. `SIGKILL` received, the
migration process would stop *forcefully*, not
*gracefully* - then there is no guarantee for
the ongoing migration process to be persisted
properly!

In your persistence callback, be sure to use
our provided `ObjectMapper` as demonstrated
in the previous section.

## Migration Progress JSON Format

In fact, `mjg` doesn't care whether you save or
load the *migration progress* to/from JSON
or not. It just:

1. Receives a `MigrationProgress` object from you,
    when you call `migrationService.run()`, and

2. Calls you (your persistence callback)
    to save the current `MigrationProgress`
    object when necessary, especially upon some
    crucial events, as discussed in the previous
    section.

But if you save and load it as JSON, then we
could talk about what it looks like when
it's serialized to JSON. Following is its schema,
conveniently written as a TypeScript type
declaration.

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
            "succeededRecordIds": Array<string | number /* int, long */>,
            "failedRecords": Array<{
                "id": string | number /* int, long */,
                "description": string,
                "cause": string,
                "action": "IGNORE" | "RETRY",
                "timestamp": string,
            }>,
        },
    },
    
    "fataLErrors": Array<string>,
}
```

Each migration class is identified by its FQCN
(Java Fully Qualified Class Name).

## Back to our Original Question

Back to our question from the very beginning
of this article: What about some failed records
that could be **retried later**, even *after*
the program has exited ?

We'll see how to monitor failed records when
we have our *migration progress* persisted
to a JSON file, which should have the same
structure/schema as seen above.

All you need to do is to read that JSON
file and act something!

For resolving errors, focus on the `failedRecords`
field of each migration class under `migrationProgress`,
which lists all the failed records during the
process of running that particular migration
class.

For each such record, you could review
its ID, description and even the cause of the
error (i.e. the thrown exception's stack trace),
along with other data and metadata.

Then, you specify an action for
the `MigrationService` to take next time
upon this particular record: set `action`
to either `IGNORE` or `RETRY`. The default
action is `RETRY`.

If you set the action to `IGNORE`, the
record will be ignored next time, i.e.,
not retried, so not migrated. Only if you set
it to `RETRY` (which is the default value)
will the `MigrationService` give it another try
next time.

In fact, most of the time, it might
just be due to some exception thrown in your
`@Migration` classes' methods, e.g. `startReduction`,
`matchXXX`, `reduceXXX`, etc. If that is the
case, fix your code, leave the action to
be `RETRY`, and simply rerun the migration
with the altered migration progress JSON
data. How robust is that! I believe that's
how a data migration program should be - extremely
resilient, robust and having a great deal of
fault-tolerance.

That's when you (de)serialize it to/from
JSON. The same principle applies if you
(de)serialize it to/from a different format.
As mentioned earlier, `mjg` doesn't care
how you save/load a `MigrationProgress`
instance.

## See also

- [Home](./README.md)
- [Previous: Fault Tolerance](./007-fault-tolerance.md)
- [Next: Datasource-specific Notes](./009-datasource-specific-notes.md)
