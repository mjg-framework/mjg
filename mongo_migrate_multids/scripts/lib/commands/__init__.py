from .add_entity import AddEntityCommand
from ..context import Context

COMMANDS = {
    "entity": {
        "add": AddEntityCommand,
    }
}

def run_from_command_line(context, argv):
    # type: (Context, list[str]) -> None

    if len(argv) < 2:
        print("Available commands:")
        print(COMMANDS)
        return 1
    else:
        c = COMMANDS
        i_command = 1
        while isinstance(c, dict):
            token = argv[i_command]
            try:
                c = c[token]
            except KeyError:
                raise RuntimeError(f"No such command.")
            i_command += 1
        
        if isinstance(c, dict):
            raise RuntimeError(f"Malformed command.")
        
        args = argv[i_command:]

        print(f"Executing command: {c.__name__}")
        print(f"Arguments: {args}")
        print()
        print("============================")
        print()

        c(context, args).run()
