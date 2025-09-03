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
      uri: mongodb://localhost:27017/eos
    dest:
      uri: mongodb://localhost:27017/eos_metadata_target
```

## Build

```sh
mvn clean compile test-compile test package
```

Or if you prefer to not run tests:

```sh
mvn -DskipTests clean compile test-compile package
```

## Run

```sh
java -jar mongo_migrate_multids/target/mongo_migrate_multids-0.0.1-SNAPSHOT.jar
```
