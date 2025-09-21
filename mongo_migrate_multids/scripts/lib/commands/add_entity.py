from ..context import Context
from typing import Any

from ..code_generators.generate_entity_class import EntityClassGenerator
from ..code_generators.generate_repository_interfaces import RepositoryInterfacesGenerator
from ..code_generators.generate_datastore_classes import DatastoreClassesGenerator

class AddEntityCommand:
    def __init__(self, context, args):
        # type: (AddEntityCommand, Context, list[Any]) -> None
        if len(args) < 3:
            raise RuntimeError(f"Specify (1) the collection/table name, (2) the entity raw name, and (3) the entity ID type, e.g. stations Station String")
        
        self.context = context
        self.table_name = args[0]
        self.raw_entity_name = args[1]
        self.entity_id_type = args[2]

        self.generators = [
            EntityClassGenerator(
                context=context,
                raw_entity_name=self.raw_entity_name,
                table_name=self.table_name,
            ),

            RepositoryInterfacesGenerator(
                context=context,
                raw_entity_name=self.raw_entity_name,
                entity_id_type=self.entity_id_type,
            ),

            DatastoreClassesGenerator(
                context=context,
                raw_entity_name=self.raw_entity_name,
                entity_id_type=self.entity_id_type,
            ),
        ]
    
    def run(self):
        for g in self.generators:
            g.run()
        
        print(f"Done. Next steps:")
        print(f"1. Go to the entity file and add fields.")
        print(f"2. Link the new datastores in the services e.g. MigrateToTWService.")
