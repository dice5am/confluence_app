"""Tests for MD-1.10 health HTTP scaffold."""

from __future__ import annotations

import json
import threading
import unittest
from urllib.request import urlopen

from market_data.server import create_server


class HealthServerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.httpd = create_server("127.0.0.1", 0)
        self.port = self.httpd.server_address[1]
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self) -> None:
        self.httpd.shutdown()
        self.httpd.server_close()

    def test_health_ok(self) -> None:
        with urlopen(f"http://127.0.0.1:{self.port}/health", timeout=5) as resp:
            body = json.loads(resp.read().decode())
            self.assertEqual(resp.status, 200)
        self.assertEqual(body["status"], "ok")
        self.assertEqual(body["venue"], "binance")
        self.assertEqual(body["symbol"], "BTCUSDT")
        self.assertIn("1m", body["activeTimeframes"])

    def test_history_stub(self) -> None:
        from urllib.error import HTTPError

        with self.assertRaises(HTTPError) as ctx:
            urlopen(f"http://127.0.0.1:{self.port}/v1/history", timeout=5)
        self.assertEqual(ctx.exception.code, 501)


if __name__ == "__main__":
    unittest.main()
