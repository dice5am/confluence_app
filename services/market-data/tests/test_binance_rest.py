"""Unit tests for MD-1.2 Binance REST klines — fixture only, no live network."""

from __future__ import annotations

import io
import json
import unittest
from pathlib import Path
from typing import Any

from market_data.binance_rest import (
    BinanceRestClient,
    map_rest_kline,
    paginate_klines,
)
from market_data.errors import BanError, RateLimitError, ServerError, TimeoutError

FIXTURES = Path(__file__).parent / "fixtures"


def _load_sample() -> list[list[Any]]:
    return json.loads((FIXTURES / "binance_klines_sample.json").read_text(encoding="utf-8"))


class _FakeHTTPResponse:
    def __init__(self, body: bytes, status: int = 200, headers: dict | None = None):
        self._body = body
        self.status = status
        self.headers = headers or {}

    def read(self) -> bytes:
        return self._body

    def __enter__(self) -> "_FakeHTTPResponse":
        return self

    def __exit__(self, *args: Any) -> None:
        return None


class MapRestKlineTests(unittest.TestCase):
    def test_maps_md11_fields(self) -> None:
        row = _load_sample()[0]
        c = map_rest_kline(row, timeframe="1h", ingest_ts_ms=999)
        self.assertEqual(c.venue, "binance")
        self.assertEqual(c.symbol, "BTCUSDT")
        self.assertEqual(c.timeframe, "1h")
        self.assertEqual(c.openTimeMs, 1704067200000)
        self.assertEqual(c.closeTimeMs, 1704070799999)
        self.assertEqual(c.open, 42000.0)
        self.assertEqual(c.high, 42500.5)
        self.assertEqual(c.low, 41800.25)
        self.assertEqual(c.close, 42250.75)
        self.assertEqual(c.volume, 12.345)
        self.assertTrue(c.isFinal)
        self.assertEqual(c.sourceTsMs, 1704070799999)
        self.assertEqual(c.ingestTsMs, 999)

    def test_to_dict_keys(self) -> None:
        c = map_rest_kline(_load_sample()[0], timeframe="1m")
        keys = set(c.to_dict())
        expected = {
            "venue",
            "symbol",
            "timeframe",
            "openTimeMs",
            "closeTimeMs",
            "open",
            "high",
            "low",
            "close",
            "volume",
            "isFinal",
            "sourceTsMs",
            "ingestTsMs",
        }
        self.assertEqual(keys, expected)


class FetchKlinesTests(unittest.TestCase):
    def test_fetch_parses_json(self) -> None:
        sample = _load_sample()
        body = json.dumps(sample).encode()

        def opener(req: Any, timeout_s: float) -> _FakeHTTPResponse:
            self.assertIn("/api/v3/klines", req.full_url)
            self.assertIn("symbol=BTCUSDT", req.full_url)
            self.assertIn("interval=1h", req.full_url)
            return _FakeHTTPResponse(body)

        client = BinanceRestClient(opener=opener)
        rows = client.fetch_klines(timeframe="1h", limit=3)
        self.assertEqual(len(rows), 3)
        self.assertEqual(rows[0][0], 1704067200000)

    def test_429_rate_limit(self) -> None:
        import urllib.error

        def opener(req: Any, timeout_s: float) -> Any:
            raise urllib.error.HTTPError(
                req.full_url, 429, "Too Many", {"Retry-After": "2"}, io.BytesIO(b"rate")
            )

        client = BinanceRestClient(opener=opener)
        with self.assertRaises(RateLimitError) as ctx:
            client.fetch_klines(timeframe="1m")
        self.assertEqual(ctx.exception.retry_after_s, 2.0)

    def test_418_ban(self) -> None:
        import urllib.error

        def opener(req: Any, timeout_s: float) -> Any:
            raise urllib.error.HTTPError(
                req.full_url, 418, "Teapot", {}, io.BytesIO(b"banned")
            )

        client = BinanceRestClient(opener=opener)
        with self.assertRaises(BanError):
            client.fetch_klines(timeframe="1m")

    def test_5xx(self) -> None:
        import urllib.error

        def opener(req: Any, timeout_s: float) -> Any:
            raise urllib.error.HTTPError(
                req.full_url, 503, "Unavailable", {}, io.BytesIO(b"down")
            )

        client = BinanceRestClient(opener=opener)
        with self.assertRaises(ServerError) as ctx:
            client.fetch_klines(timeframe="1m")
        self.assertEqual(ctx.exception.status_code, 503)

    def test_timeout(self) -> None:
        import urllib.error

        def opener(req: Any, timeout_s: float) -> Any:
            raise urllib.error.URLError("timed out")

        client = BinanceRestClient(opener=opener)
        with self.assertRaises(TimeoutError):
            client.fetch_klines(timeframe="1m")


class PaginateTests(unittest.TestCase):
    def test_paginate_advances_cursor(self) -> None:
        page1 = _load_sample()[:2]
        page2 = _load_sample()[2:]
        calls: list[dict[str, Any]] = []

        class Stub(BinanceRestClient):
            def fetch_klines(self, **kwargs: Any) -> list[list[Any]]:  # type: ignore[override]
                calls.append(kwargs)
                if len(calls) == 1:
                    return page1
                return page2

        candles = paginate_klines(
            Stub(),
            timeframe="1h",
            start_ms=1704067200000,
            end_ms=1704074400000,
            limit=2,
        )
        self.assertEqual(len(candles), 3)
        self.assertTrue(all(c.isFinal for c in candles))
        self.assertEqual(calls[1]["start_ms"], 1704070800000 + 1)
        self.assertEqual(candles[0].openTimeMs, 1704067200000)
        self.assertEqual(candles[-1].openTimeMs, 1704074400000)


if __name__ == "__main__":
    unittest.main()
