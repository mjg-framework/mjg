from typing import Any

class StringBuilder:
    def __init__(self):
        self.buf = "" # type: str

    def add_lines(self, *lines, **options):
        # type: (StringBuilder, list[str|None], dict[str, Any]) -> StringBuilder
        try:
            indentation = ""
            indent_size = options.get("indent_size", 0)
            if indent_size > 0:
                indentation = " " * indent_size
            
            for line in lines:
                if not line or not line.strip():
                    line = ""
                else:
                    line = indentation + line
                self.buf += line + '\n'
            return self
        except Exception as e:
            raise Exception(e, lines)
    
    def build(self):
        return self.buf
