from ...context import Context

from .generate_base_datastore import BaseDatastoreGenerator
from .generate_src_datastore import SrcDatastoreGenerator
from .generate_dest_datastore import DestDatastoreGenerator

class DatastoreClassesGenerator:
    def __init__(self, context, raw_entity_name, entity_id_type):
        # type: (DatastoreClassesGenerator, Context, str, str) -> None
        self.context = context

        self.generators = [
            BaseDatastoreGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
                entity_id_type=entity_id_type,
            ),

            SrcDatastoreGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
            ),

            DestDatastoreGenerator(
                context=context,
                raw_entity_name=raw_entity_name,
            ),
        ]
    
    def run(self):
        return [g.run() for g in self.generators]
