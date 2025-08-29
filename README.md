# Mongo Migrate (Multi-datasource) – Spring Boot

Connects to two MongoDB databases at once and migrates collections while generating new _id in target.

## Requirements
- Java 17+
- Maven
- MongoDB

## Configure
Edit `src/main/resources/application.yml`:

```yaml
data:
  mongodb:
    source:
      uri: mongodb://localhost:27017
      database: sourceDB
    target:
      uri: mongodb://localhost:27017
      database: targetDB
```

## Build & Run
```bash
mvn -DskipTests clean compile test-compile package
java -jar mongo_migrate_multids/target/mongo_migrate_multids-0.0.1-SNAPSHOT.jar
```

By default, `MigrateRunner` migrates the `users` collection using `MigrationService`.
Update `MigrateRunner` or create your own runner/service to select collections and logic.
