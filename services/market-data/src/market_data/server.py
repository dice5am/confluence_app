"""MD-1.10 — Minimal HTTP server: GET /health (+ stub routes for future)."""

from __future__ import annotations

import json
import logging
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import urlparse

from .candle import TIMEFRAMES, Health

logger = logging.getLogger(__name__)

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8080


class MarketDataHandler(BaseHTTPRequestHandler):
    server_version = "MarketDataScaffold/0.1"

    def log_message(self, fmt: str, *args: Any) -> None:
        logger.info("%s - %s", self.address_string(), fmt % args)

    def _json(self, status: int, body: dict[str, Any]) -> None:
        raw = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self) -> None:  # noqa: N802 — BaseHTTPRequestHandler API
        path = urlparse(self.path).path.rstrip("/") or "/"

        if path == "/health":
            health = Health(
                status="ok",
                lastSourceTsMs=0,
                venue="binance",
                symbol="BTCUSDT",
                activeTimeframes=list(TIMEFRAMES),
                note="scaffold — no live ingest wired yet",
            )
            self._json(200, health.to_dict())
            return

        # Stubs for future MD history/live (documented; not implemented in P1 scaffold)
        if path == "/v1/history":
            self._json(
                501,
                {
                    "error": "not_implemented",
                    "message": "GET /v1/history stub — Phase 1 scaffold only",
                },
            )
            return

        if path == "/v1/live":
            self._json(
                501,
                {
                    "error": "not_implemented",
                    "message": "GET /v1/live stub — use WS ingest later; Phase 1 scaffold only",
                },
            )
            return

        self._json(404, {"error": "not_found", "path": path})


def create_server(host: str = DEFAULT_HOST, port: int = DEFAULT_PORT) -> ThreadingHTTPServer:
    return ThreadingHTTPServer((host, port), MarketDataHandler)


def serve_forever(host: str = DEFAULT_HOST, port: int = DEFAULT_PORT) -> None:
    httpd = create_server(host, port)
    logger.info("listening on http://%s:%s  GET /health", host, port)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("shutting down")
    finally:
        httpd.server_close()
