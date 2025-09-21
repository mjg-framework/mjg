from ...string_builder import StringBuilder
from ...context import Context

class GenericDatastoreClassGenerator:
    def __init__(
        self, context, name, preamble, file_path,
        annotations, extends_or_implements,
        is_abstract, body
    ):
        # type: (GenericDatastoreClassGenerator, Context, str, str, str, str, str, bool, str) -> None
        self.context = context
        self.name = name
        self.preamble = preamble
        self.file_path = file_path
        self.annotations = annotations
        self.extends_or_implements = extends_or_implements
        self.is_abstract = is_abstract
        self.body = body
    
    def run(self):
        name = self.name
        preamble = self.preamble
        file_path = self.file_path
        annotations = self.annotations
        extends_or_implements = self.extends_or_implements
        is_abstract = self.is_abstract
        body = self.body

        with open(file_path, 'x') as f:
            content = StringBuilder().add_lines(
                preamble,
                f'',
                annotations,
                f'public{" abstract" if is_abstract else ""} class {name}',
                extends_or_implements,
                "{",
                body,
                "}",
            ).build()
            f.write(content)
