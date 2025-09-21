from .generic import GenericRepositoryInterfaceGenerator
from ...string_builder import StringBuilder
from ...context import Context

class DestRepositoryGenerator:
    def __init__(self, context, raw_entity_name, entity_id_type):
        # type: (DestRepositoryGenerator, Context, str, str) -> None
        C = context
        self.context = context

        entity_name = f"{raw_entity_name}Entity"
        base_repository_name = f"Base{raw_entity_name}Repository"
        repo_name = f'Dest{raw_entity_name}Repository'
        file_path = f"{C.ROOT_MODULE_DIR}/repository/dest/{repo_name}.java"

        preamble = StringBuilder().add_lines(
            f'package {C.ROOT_MODULE_FQN}.repository.dest;'
            f'',
            f'import org.springframework.stereotype.Repository;',
            f'import {C.ROOT_MODULE_FQN}.entity.{entity_name};',
            f'import {C.ROOT_MODULE_FQN}.repository.common.{base_repository_name};',
            f'import {C.ROOT_MODULE_FQN}.repository.dest.common.DestMongoRepositoryInterface;',
        ).build()

        annotations = '@Repository'

        extends = StringBuilder().add_lines(
            f'extends',
            f'    {base_repository_name},',
            f'    DestMongoRepositoryInterface<{entity_name}, {entity_id_type}>',
        ).build()

        self.g = GenericRepositoryInterfaceGenerator(
            context=context,
            name=repo_name,
            preamble=preamble,
            file_path=file_path,
            annotations=annotations,
            extends_or_implements=extends,
        )
    
    def run(self):
        return self.g.run()
