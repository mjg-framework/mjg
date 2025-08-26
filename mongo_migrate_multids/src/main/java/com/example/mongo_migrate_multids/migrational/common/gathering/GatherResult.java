package com.example.mongo_migrate_multids.migrational.common.gathering;

import java.util.stream.Stream;

public interface GatherResult<T> {
    Stream<T> getDataStream();

    boolean isFinished();
}
