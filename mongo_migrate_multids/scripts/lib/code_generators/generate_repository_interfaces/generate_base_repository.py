from .generic import GenericRepositoryInterfaceGenerator
from ...string_builder import StringBuilder
from ...context import Context

class BaseRepositoryGenerator:
    def __init__(self, context, raw_entity_name, entity_id_type):
        # type: (BaseRepositoryGenerator, Context, str, str) -> None
        C = context
        self.context = context

        repo_name = f'Base{raw_entity_name}Repository'
        entity_name = f"{raw_entity_name}Entity"
        file_path = f"{C.ROOT_MODULE_DIR}/repository/common/{repo_name}.java"

        preamble = StringBuilder().add_lines(
            f'package {C.ROOT_MODULE_FQN}.repository.common;'
            f'',
            f'import com.example.mjg.spring.mongo.repositories.MigratableMongoRepository;',
            f'import {C.ROOT_MODULE_FQN}.entity.{entity_name};',
        ).build()

        extends = StringBuilder().add_lines(
            f'extends MigratableMongoRepository<{entity_name}, {entity_id_type}>'
        ).build()

        self.g = GenericRepositoryInterfaceGenerator(
            context=context,
            name=repo_name,
            preamble=preamble,
            file_path=file_path,
            annotations="",
            extends_or_implements=extends,
        )
    
    def run(self):
        return self.g.run()
