# Fault Tolerance

If some exception is thrown
during the migration progress
of one input record - whether
it is in the phase of loading,
matching, reduction, transformation,
or saving the output records - `mjg`
could retry migrating that specific
input record *for some times*
before declaring it as failed
to migrate.

By default, the framework retries
only once, 1 seconds since the point
of failure. You could customize
it to your liking. Just specify
the retry times (`retryTimes`) and
delay between retries (`retryDelayInSeconds`)
in `@ForEachRecordFrom`, `@MatchWith`,
and `@TransformAndSaveTo`, like
in the following example:

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

## See also

- [Home](./README.md)
- [Previous: Advanced Example - Chaining Migrations](./006-chaining-migrations.md)
- [Next: Migration Progress JSON format](./008-migration-progress-json-format.md)
