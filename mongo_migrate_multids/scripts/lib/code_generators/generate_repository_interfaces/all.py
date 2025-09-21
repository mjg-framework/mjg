from ...context import Context

from .generate_base_repository import BaseRepositoryGenerator
from .generate_src_repository import SrcRepositoryGenerator
from .generate_dest_repository import DestRepositoryGenerator

class RepositoryInterfacesGenerator:
    def __init__(self, context, raw_entity_name, entity_id_type):
        # type: (RepositoryInterfacesGenerator, Context, str, str) -> None
        self.context = context

        self.generators = [
            BaseRepositoryGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
                entity_id_type=entity_id_type,
            ),

            SrcRepositoryGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
                entity_id_type=entity_id_type,
            ),

            DestRepositoryGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
                entity_id_type=entity_id_type,
            ),
        ]
    
    def run(self):
        return [g.run() for g in self.generators]
