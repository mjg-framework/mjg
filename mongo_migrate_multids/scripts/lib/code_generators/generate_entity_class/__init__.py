import os

from ...string_builder import StringBuilder
from ...context import Context

class EntityClassGenerator:
    def __init__(self, context, raw_entity_name, table_name):
        # type: (EntityClassGenerator, Context, str, str) -> None
        self.context = context
        self.raw_entity_name = raw_entity_name
        self.table_name = table_name
    
    def run(self):
        C = self.context
        entity_name = f"{self.raw_entity_name}Entity"
        entity_file_path = os.path.join(C.ROOT_MODULE_DIR, f"entity/{entity_name}.java")
        with open(entity_file_path, 'x') as f:
            content = StringBuilder().add_lines(
                f"package {C.ROOT_MODULE_FQN}.entity;",
                f"",
                f"import org.springframework.data.annotation.Id;",
                f"import org.springframework.data.mongodb.core.mapping.Document;",
                f"import org.springframework.data.mongodb.core.mapping.Field;",
                f"import com.example.mjg.data.MigratableEntity;",
                f"import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;"
                f"import lombok.Getter;",
                f"import lombok.Setter;",
                f"import java.io.Serializable;",
                f"",
                f"",
                f'@Document(value = "{self.table_name}")',
                f'@Getter',
                f'@Setter',
                f'public class {entity_name} implements MigratableEntity {{',
                f'    @Override',
                f'    public Serializable getMigratableId() {{',
                f'        return id;',
                f'    }}',
                f'',
                f'    @Override',
                f'    public String getMigratableDescription() {{',
                f'        return "{self.raw_entity_name}(id=" + id + ", largeInteger=" + (',
                f'            id == null ? "null" : ObjectIdHelpers.convertObjectIdToLargeInteger(id)',
                f'        )',
                f'            + ")";',
                f'    }}',
                f'',
                f'    @Id',
                f'    public String id;',
                f'',
                f'    // TODO: Add your fields',
                f'    @Field(value = "YOUR_FIELD_HEREEEEEE")',
                f'    public String YOUR_FIELD_HEREEEEEE;',
                f'',
                f'    public {entity_name} copyAllExceptId() {{',
                f'        {entity_name} copy = new {entity_name}();',
                f'        // TODO: Copy the fields',
                f'        return copy;',
                f'    }}',
                f'}}',
            ).build()

            f.write(content)
