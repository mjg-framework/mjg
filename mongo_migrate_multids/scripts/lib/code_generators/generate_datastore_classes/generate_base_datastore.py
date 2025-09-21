from .generic import GenericDatastoreClassGenerator
from ...string_builder import StringBuilder
from ...context import Context

class BaseDatastoreGenerator:
    def __init__(self, context, raw_entity_name, entity_id_type):
        # type: (BaseDatastoreGenerator, Context, str, str) -> None
        C = context
        self.context = context

        store_name = f'Base{raw_entity_name}Store'
        file_path = f"{C.ROOT_MODULE_DIR}/migrational/datastores/common/{store_name}.java"
        entity_name = f"{raw_entity_name}Entity"

        preamble = StringBuilder().add_lines(
            f'package {C.ROOT_MODULE_FQN}.migrational.datastores.common;',
            f'',
            f'import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;',
            f'import {C.ROOT_MODULE_FQN}.entity.{entity_name};',
        ).build()

        extends = StringBuilder().add_lines(
            f'extends MongoRepositoryStore<{entity_name}, {entity_id_type}>'
        ).build()

        self.g = GenericDatastoreClassGenerator(
            context=context,
            name=store_name,
            preamble=preamble,
            file_path=file_path,
            annotations="",
            extends_or_implements=extends,
            is_abstract=True,
            body="",
        )
    
    def run(self):
        return self.g.run()
