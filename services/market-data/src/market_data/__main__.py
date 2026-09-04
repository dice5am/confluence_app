"""python -m market_data — run health HTTP server."""

from __future__ import annotations

import logging
import os

from .server import DEFAULT_HOST, DEFAULT_PORT, serve_forever


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    host = os.environ.get("MD_HOST", DEFAULT_HOST)
    port = int(os.environ.get("MD_PORT", str(DEFAULT_PORT)))
    serve_forever(host, port)


if __name__ == "__main__":
    main()
