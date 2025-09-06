# Overview

- [Overview](#overview)
  - [What is mjg](#what-is-mjg)
  - [Features](#features)
  - [Terminology](#terminology)
    - [Data Source](#data-source)
    - [Data Store](#data-store)
    - [Record](#record)
    - [Migration](#migration)
    - [Spring Repository](#spring-repository)
    - [Annotations](#annotations)
  - [Progress Persistence](#progress-persistence)
  - [See also](#see-also)

## What is mjg

It is a Data Migration framework for Java applications.
It helps you move data from one location to another
with ease, even data with references (e.g. foreign
keys). Because of this, `mjg` could be used as a
complete [ETL/ELT solution](https://en.wikipedia.org/wiki/Extract,_transform,_load).

A lot of implementation details e.g. batching,
fault tolerance and saved progress are
automatically handled, so that you can focus
solely on the business logic of your migration
processes, with minimal concern about data
corruption.

`mjg` could work with any data source that
supports:

- **Paging/Batching:** load data by page number
    (starts at 0) and page size.
- **Record Lookup by ID:** look up records by ID. The type
    of ID must extend the `java.io.Serializable` interface.
    It could be `String`, `Integer`, etc.
- Save multiple records in a transaction, i.e. either
    all those records are saved successfully, or none
    at all. Also known as All-or-nothing transaction.

    **Support for this is optional**, though it is nice
    to have and could speed up the migration process
    while still maintaining stability.
    
    **If it is not supported, there will be quirks to**
    **watch out for,** [as described in a later article](./009-datasource-specific-notes.md).

Right now, adapters for Spring repositories
in general, and `spring-data-mongodb` repositories
have been implemented.

This series of articles focus on creating and
running migrations among Spring repositories,
though the idea is pretty much the same with
any eligible data sources.

## Features

- **Automatic batching:** Always load, process and save
    data batch by batch.
    
    The default batch size is 512.

    You can customize it for each phase (load/process/
    save) of your migrations.

- **Progress persistence:** When running migrations, the
    framework maintains a data structure called *migration*
    *progress* to remember all successfully migrated records,
    as well as records that fail to migrate, along with
    the causes of the failures.

    The user chooses where to save and load the data from. It
    is JSON serializable.

- **Fault tolerance:** The framework may try to migrate
    failed records again for some times before finally
    report it as failed to migrate.
    
    The number of retry times and delay between retries
    are customizable for each phase (load/process/save)
    as well.
    
    By default, the framework retries only once, 1 seconds
    since the point of failure.
    
    Failed records are saved in *migration progress* and
    could be retried in the next run (by default) or ignored
    (if the user decides that record to be ignored).

- **Automatic migration ordering/chaining:** the order of
    execution of several *migrations* (see terminology below)
    could be deduced automatically, ensuring the up-to-date-ness
    and validity of migrated data.

    The order of migrations can also be rearranged by hand.

    Details on this will be discussed [in a later article](./006-chaining-migrations.md).

## Terminology

### Data Source

A data source is anywhere data could
be read from or write to. For example,
a MySQL database connection, a MongoDB
database connection, a Redis connection,
an open file, an open socket, etc.

### Data Store

A data source consists of one or multiple
data stores. Each datastore contains *records*
of the same data type, or those that share
some semantics.

For SQL databases, a datastore corresponds
to an SQL table under the hood.

For MongoDB databases, a datastore corresponds
to a Mongo collection behind the scenes.

Other types of data sources have their
own notion of what its datastores represent.

### Record

A data store, as stated above, contains
records. They are the unit of data
that could be read from and write to
a datastore as a whole.

For SQL databases, a record corresponds to
one row of some table.

For MongoDB databases, a record corresponds
to a Mongo document of some collection.

Again, other types of data sources and/or
datastores have their own notion of what
a record represents.

### Migration

A migration in `mjg` is one set of operations
to load *input records* from a *source datastore*, process
them, transform them into *output records* to be
saved to a *destination datastore*.

Put simply, it is basically moving data from one
place to another, with some processing as necessary.

While processing, a migration could also pull
records from relevant *matching datastores* in
certain use cases. For example, when a MySQL
table has a foreign key, it is crucial that
the same relationship is maintained when
moving data to the new place (datastore).
Details on this will be demonstrated in
a later article.

To sum up, you've encountered some terms
in this section, which will be explained
and used in subsequent articles:

- input records
- source datastore
- output records
- destination datastore
- matching datastores

You will see the first *migration* in the
very next article.

### Spring Repository

A JPA/MongoDB Spring repository usually
offers operations to read/write rows/documents
from one SQL table/MongoDB collection.

Therefore, throughout this guide, we
consider that one Spring repository
corresponds to one datastore. As you
can see in the next articles, we will
create datastores that have their
appropriate repositories injected
by Spring Boot.

### Annotations

Annotations are a feature that has been
around since Java 5.0. It allows developers
to add metadata to *program constructs*,
such as classes, methods, fields, and
parameters. Developers can then write:

- **Annotation processors** - to process those
    *program constructs* at *compile time*.
- **Reflection code** - to process those
    *program constructs* at *run time*.

Annotations are typically applied to a
*program construct* by prepending it with
the `@` sign followed by the annotation name
and arguments. A very basic example with
Lombok:

```java
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ABC { /*...*/ }
```

Here, `AllArgsConstructor` is the annotation
name, `access` is one of its *elements*,
and `AccessLevel.PRIVATE` is the argument
passed to the `access` element of the
annotation.

You could see that Spring Boot relies heavily
on annotations: remember `@Component`, `@Bean`,
`@Service`, `@Repository` ? Thanks to the
power of annotation, `mjg` use them as well!
You will see some `mjg` annotations shortly.

**Note the special "value" element:** if an
annotation has only one element, and that
element is named "value", you can omit the
element name when providing the value.
For example, using Spring Data MongoDB,
the following two code snippets are identical:

1. Provide the element name.

    ```java
    @Document(value = "areas")
    public class AreaEntity { /*...*/ }
    ```

2. Omit the element name.

    ```java
    @Document("areas")
    public class AreaEntity { /*...*/ }
    ```

## Progress Persistence

While `mjg` do support progress persistence
and continuation, it doesn't automatically
saves the progress to some arbitrary JSON
file or somewhere else when it's done.

Instead, you have to register a callback that
receives the *migration progress* data,
serialize it to JSON and save to a file.
Or you can save it anywhere else. Just make
sure that you always load the previous
progress from that same file/location in
the code that runs the migration service.

The persistence callback will automatically
be called whenever the migration progress needs
to be saved, e.g. after one migration, when
there is a fatal error, or when the user
presses Ctrl-C (send SIGTERM, SIGINT on
Linux, CTRL_C_EVENT on Windows). If the
program is killed (SIGKILL on Linux), there
is no recovery!

Following is **an example** for that so
that you **grasp the idea, no need to**
**memorize it**. The details will be covered
[in the next article](./001-simple-example.md).

At startup:

```java
import com.example.mjg.services.migration.MigrationService;
private final MigrationService migrationService;
// ...
migrationService.addProgressPersistenceCallback(saveMigrationProgress);
```

When run the migrations:

```java
MigrationProgress migrationProgress = loadMigrationProgress();
migrationService.run(migrationProgress);
```

The progress persistence callback and the progress loader method:

```java
// We intend to save it into a JSON file. Here is the path to it.
private static final String MIGRATION_PROGRESS_FILE_PATH = "/path/to/json/file/which/should/be/constant";

// The persistence callback
// Note that if you want to serialize to JSON, use the ObjectMapper
// from ObjectMapperFactory.get() like in the following code, or
// else the data might not be saved/restored properly.
private static void saveMigrationProgress(MigrationProgress migrationProgress) {
    System.out.println("============= SAVING PROGRESS ================");
    BufferedWriter writer = null;
    try {
        FileWriter fileWriter = new FileWriter(MIGRATION_PROGRESS_FILE_PATH, false);
        writer = new BufferedWriter(fileWriter);

        // import com.example.mjg.utils.ObjectMapperFactory;
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

// The progress loader method:
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

    return migrationProgress;
}
```

## See also

- [Home](./README.md)
- [Next: Simple Example](./001-simple-example.md)
