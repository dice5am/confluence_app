"""Unit + optional integration tests for MD-1.3 Binance WS klines."""

from __future__ import annotations

import json
import os
import unittest
from pathlib import Path

from market_data.binance_ws import (
    BinanceKlineWsClient,
    combined_stream_url,
    map_ws_kline_event,
    next_backoff_s,
)

FIXTURES = Path(__file__).parent / "fixtures"
RUN_INTEGRATION = os.environ.get("RUN_BINANCE_WS_INTEGRATION") == "1"


class MapWsKlineTests(unittest.TestCase):
    def test_forming_is_final_false(self) -> None:
        payload = json.loads(
            (FIXTURES / "binance_ws_kline_forming.json").read_text(encoding="utf-8")
        )
        c = map_ws_kline_event(payload, ingest_ts_ms=111)
        assert c is not None
        self.assertEqual(c.venue, "binance")
        self.assertEqual(c.symbol, "BTCUSDT")
        self.assertEqual(c.timeframe, "1m")
        self.assertEqual(c.openTimeMs, 1704074400000)
        self.assertEqual(c.closeTimeMs, 1704077999999)
        self.assertEqual(c.open, 42800.0)
        self.assertEqual(c.close, 42750.0)
        self.assertEqual(c.volume, 5.25)
        self.assertFalse(c.isFinal)
        self.assertEqual(c.sourceTsMs, 1704074500000)
        self.assertEqual(c.ingestTsMs, 111)

    def test_closed_is_final_true(self) -> None:
        payload = json.loads(
            (FIXTURES / "binance_ws_kline_final.json").read_text(encoding="utf-8")
        )
        c = map_ws_kline_event(payload)
        assert c is not None
        self.assertTrue(c.isFinal)
        self.assertEqual(c.close, 42700.5)
        self.assertEqual(c.volume, 8.5)

    def test_ignores_non_kline(self) -> None:
        self.assertIsNone(map_ws_kline_event({"e": "ping"}))


class StreamUrlTests(unittest.TestCase):
    def test_combined_url(self) -> None:
        url = combined_stream_url(("1m", "5m"), symbol="BTCUSDT")
        self.assertTrue(url.startswith("wss://stream.binance.com:9443/stream?streams="))
        self.assertIn("btcusdt@kline_1m", url)
        self.assertIn("btcusdt@kline_5m", url)


class BackoffTests(unittest.TestCase):
    def test_backoff_grows(self) -> None:
        a0 = next_backoff_s(0)
        a3 = next_backoff_s(3)
        self.assertGreater(a0, 0)
        self.assertGreaterEqual(a3, a0 * 0.5)  # jitter-tolerant


class ClientSkeletonTests(unittest.TestCase):
    def test_client_url_and_module_exists(self) -> None:
        client = BinanceKlineWsClient(timeframes=("1m", "1h"))
        self.assertIn("btcusdt@kline_1m", client.url)
        self.assertIn("btcusdt@kline_1h", client.url)


@unittest.skipUnless(RUN_INTEGRATION, "set RUN_BINANCE_WS_INTEGRATION=1 for live WS")
class LiveWsIntegrationTests(unittest.TestCase):
    def test_receive_one_kline(self) -> None:
        import threading

        received: list = []
        done = threading.Event()

        def on_candle(c: object) -> None:
            received.append(c)
            done.set()

        client = BinanceKlineWsClient(timeframes=("1m",), on_candle=on_candle)
        client.start()
        ok = done.wait(timeout=30.0)
        client.stop()
        self.assertTrue(ok, "did not receive a kline within 30s")
        self.assertGreaterEqual(len(received), 1)


if __name__ == "__main__":
    unittest.main()
