"""Dev / Termux entry point:  python -m jarvis_server  (or  python -m jarvis_server 8080)"""
from __future__ import annotations

import os
import sys

from . import create_app


def main() -> None:
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("PORT", "8787"))
    app = create_app()
    print(f"JARVIS server on http://0.0.0.0:{port}  (token: {'set' if app.extensions['jarvis'].cfg.token else 'none'})")
    app.run(host="0.0.0.0", port=port, threaded=True, debug=False)


if __name__ == "__main__":
    main()
