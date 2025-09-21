from .generic import GenericDatastoreClassGenerator
from ...string_builder import StringBuilder
from ...context import Context

class DestDatastoreGenerator:
    def __init__(self, context, raw_entity_name):
        # type: (DestDatastoreGenerator, Context, str) -> None
        C = context
        self.context = context

        base_store_name = f'Base{raw_entity_name}Store'
        store_name = f'Dest{raw_entity_name}Store'
        repo_name = f'Dest{raw_entity_name}Repository'
        file_path = f"{C.ROOT_MODULE_DIR}/migrational/datastores/dest/{store_name}.java"

        preamble = StringBuilder().add_lines(
            f'package {C.ROOT_MODULE_FQN}.migrational.datastores.dest;',
            f'',
            f'import org.springframework.data.mongodb.MongoTransactionManager;',
            f'import org.springframework.data.mongodb.core.MongoTemplate;',
            f'import org.springframework.stereotype.Component;',
            f'import com.mongodb.lang.Nullable;',
            f'import lombok.AllArgsConstructor;',
            f'import lombok.Getter;',
            f'',
            f'import {C.ROOT_MODULE_FQN}.migrational.datastores.common.{base_store_name};',
            f'import {C.ROOT_MODULE_FQN}.repository.dest.{repo_name};',
        ).build()

        annotations = StringBuilder().add_lines(
            f'@Component',
            f'@AllArgsConstructor',
        ).build()

        extends = StringBuilder().add_lines(
            f'extends {base_store_name}'
        ).build()

        body = StringBuilder().add_lines(
            f'@Getter',
            f'private final {repo_name} repository;'
            f'',
            f'@Getter',
            f'private final MongoTemplate mongoTemplate;',
            f'',
            f'@Getter',
            f'@Nullable',
            f'private final MongoTransactionManager txManager;',

            indent_size=4,
        ).build()

        self.g = GenericDatastoreClassGenerator(
            context=context,
            name=store_name,
            preamble=preamble,
            file_path=file_path,
            annotations=annotations,
            extends_or_implements=extends,
            is_abstract=False,
            body=body,
        )
    
    def run(self):
        return self.g.run()
