# Python 3.6+

import sys
import os

from lib.context import Context
from lib.commands import run_from_command_line

C = Context()
C.SCRIPT_PATH = sys.argv[0]
C.SCRIPT_DIR = os.path.dirname(C.SCRIPT_PATH)

C.ROOT_MODULE_NAME = "mongo_migrate_multids"
C.ROOT_MODULE_FQN = f"com.example.{C.ROOT_MODULE_NAME}" # fully-qualified module name
C.ROOT_MODULE_DIR = os.path.abspath(os.path.join(C.SCRIPT_DIR, f"../src/main/java/com/example/{C.ROOT_MODULE_NAME}/"))

def main():
    assert os.path.isdir(C.ROOT_MODULE_DIR), f"Invalid root module directory: {C.ROOT_MODULE_DIR}"

    run_from_command_line(C, sys.argv)

if __name__ == "__main__":
    main()
