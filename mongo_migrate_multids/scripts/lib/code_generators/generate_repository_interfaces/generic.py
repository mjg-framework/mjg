from ...string_builder import StringBuilder
from ...context import Context

class GenericRepositoryInterfaceGenerator:
    def __init__(
        self, context, name, preamble, file_path,
        annotations, extends_or_implements
    ):
        # type: (GenericRepositoryInterfaceGenerator, Context, str, str, str, str, str) -> None
        self.context = context
        self.name = name
        self.preamble = preamble
        self.file_path = file_path
        self.annotations = annotations
        self.extends_or_implements = extends_or_implements

    def run(self):
        repo_name = self.name
        preamble = self.preamble
        file_path = self.file_path
        annotations = self.annotations
        extends_or_implements = self.extends_or_implements

        with open(file_path, 'x') as f:
            content = StringBuilder().add_lines(
                preamble,
                f'',
                annotations,
                f'public interface {repo_name}',
                extends_or_implements,
                "{}",
            ).build()
            f.write(content)
