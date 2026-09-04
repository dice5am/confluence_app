"""MD-1.2 — Binance public REST klines (no API key)."""

from __future__ import annotations

import json
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Callable, Sequence

from .candle import BINANCE_INTERVAL, TIMEFRAMES, Candle, Timeframe
from .errors import BanError, NetworkError, RateLimitError, ServerError, TimeoutError

BINANCE_REST_BASE = "https://api.binance.com"
KLINES_PATH = "/api/v3/klines"
DEFAULT_SYMBOL = "BTCUSDT"
MAX_LIMIT = 1000

# Binance kline row indices
_I_OPEN_TIME = 0
_I_OPEN = 1
_I_HIGH = 2
_I_LOW = 3
_I_CLOSE = 4
_I_VOLUME = 5
_I_CLOSE_TIME = 6


def map_rest_kline(
    row: Sequence[Any],
    *,
    timeframe: Timeframe,
    symbol: str = DEFAULT_SYMBOL,
    venue: str = "binance",
    is_final: bool = True,
    source_ts_ms: int | None = None,
    ingest_ts_ms: int | None = None,
) -> Candle:
    """Map one Binance REST kline array to MD-1.1 Candle.

    Closed REST history rows use isFinal=True. Callers may set is_final=False
    only for an intentionally open last bar if ever returned.
    """
    now = int(time.time() * 1000)
    open_time = int(row[_I_OPEN_TIME])
    close_time = int(row[_I_CLOSE_TIME])
    return Candle(
        venue=venue,  # type: ignore[arg-type]
        symbol=symbol,
        timeframe=timeframe,
        openTimeMs=open_time,
        closeTimeMs=close_time,
        open=float(row[_I_OPEN]),
        high=float(row[_I_HIGH]),
        low=float(row[_I_LOW]),
        close=float(row[_I_CLOSE]),
        volume=float(row[_I_VOLUME]),
        isFinal=is_final,
        sourceTsMs=source_ts_ms if source_ts_ms is not None else close_time,
        ingestTsMs=ingest_ts_ms if ingest_ts_ms is not None else now,
    )


class BinanceRestClient:
    """Public REST client for /api/v3/klines. No API secrets."""

    def __init__(
        self,
        *,
        base_url: str = BINANCE_REST_BASE,
        timeout_s: float = 15.0,
        opener: Callable[[urllib.request.Request, float], Any] | None = None,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s
        self._opener = opener or self._default_open

    @staticmethod
    def _default_open(req: urllib.request.Request, timeout_s: float) -> Any:
        return urllib.request.urlopen(req, timeout=timeout_s)

    def fetch_klines(
        self,
        *,
        symbol: str = DEFAULT_SYMBOL,
        timeframe: Timeframe,
        start_ms: int | None = None,
        end_ms: int | None = None,
        limit: int = MAX_LIMIT,
    ) -> list[list[Any]]:
        """Fetch one page of raw Binance klines."""
        if timeframe not in TIMEFRAMES:
            raise ValueError(f"unsupported timeframe: {timeframe}")
        if limit < 1 or limit > MAX_LIMIT:
            raise ValueError(f"limit must be 1..{MAX_LIMIT}")

        params: dict[str, str] = {
            "symbol": symbol,
            "interval": BINANCE_INTERVAL[timeframe],
            "limit": str(limit),
        }
        if start_ms is not None:
            params["startTime"] = str(start_ms)
        if end_ms is not None:
            params["endTime"] = str(end_ms)

        qs = urllib.parse.urlencode(params)
        url = f"{self.base_url}{KLINES_PATH}?{qs}"
        req = urllib.request.Request(url, method="GET", headers={"Accept": "application/json"})

        try:
            with self._opener(req, self.timeout_s) as resp:
                body = resp.read()
                status = getattr(resp, "status", 200)
                if status != 200:
                    self._raise_http(status, body, resp)
                return json.loads(body.decode("utf-8"))
        except RateLimitError:
            raise
        except BanError:
            raise
        except ServerError:
            raise
        except TimeoutError:
            raise
        except urllib.error.HTTPError as e:
            body = e.read() if hasattr(e, "read") else b""
            self._raise_http(e.code, body, e)
        except urllib.error.URLError as e:
            reason = str(getattr(e, "reason", e))
            if "timed out" in reason.lower():
                raise TimeoutError(reason) from e
            raise NetworkError(reason) from e
        except TimeoutError:
            raise
        except Exception as e:
            # socket.timeout and friends
            name = type(e).__name__
            if "timeout" in name.lower() or "timed out" in str(e).lower():
                raise TimeoutError(str(e)) from e
            raise

    def _raise_http(self, status: int, body: bytes, resp: Any) -> None:
        text = body.decode("utf-8", errors="replace")[:500]
        if status == 429:
            retry_after = None
            headers = getattr(resp, "headers", None)
            if headers is not None:
                ra = headers.get("Retry-After")
                if ra:
                    try:
                        retry_after = float(ra)
                    except ValueError:
                        retry_after = None
            raise RateLimitError(f"429: {text}", retry_after_s=retry_after)
        if status == 418:
            raise BanError(f"418: {text}")
        if 500 <= status <= 599:
            raise ServerError(f"{status}: {text}", status_code=status)
        raise MarketDataHttpError(status, text)


class MarketDataHttpError(Exception):
    def __init__(self, status_code: int, message: str):
        super().__init__(f"HTTP {status_code}: {message}")
        self.status_code = status_code


def paginate_klines(
    client: BinanceRestClient,
    *,
    symbol: str = DEFAULT_SYMBOL,
    timeframe: Timeframe,
    start_ms: int,
    end_ms: int,
    limit: int = MAX_LIMIT,
    sleep_s: float = 0.0,
) -> list[Candle]:
    """Paginate closed klines from start_ms through end_ms (inclusive window).

    Advances startTime past the last openTimeMs + 1 to avoid duplicates.
    All returned candles have isFinal=True (REST closed rows).
    """
    out: list[Candle] = []
    cursor = start_ms
    ingest = int(time.time() * 1000)

    while cursor <= end_ms:
        rows = client.fetch_klines(
            symbol=symbol,
            timeframe=timeframe,
            start_ms=cursor,
            end_ms=end_ms,
            limit=limit,
        )
        if not rows:
            break

        for row in rows:
            candle = map_rest_kline(
                row,
                timeframe=timeframe,
                symbol=symbol,
                is_final=True,
                ingest_ts_ms=ingest,
            )
            if candle.openTimeMs > end_ms:
                return out
            out.append(candle)

        last_open = int(rows[-1][_I_OPEN_TIME])
        next_cursor = last_open + 1
        if next_cursor <= cursor:
            break
        cursor = next_cursor

        if len(rows) < limit:
            break
        if sleep_s > 0:
            time.sleep(sleep_s)

    return out


def fetch_btc_all_timeframes(
    client: BinanceRestClient | None = None,
    *,
    limit: int = 5,
) -> dict[Timeframe, list[Candle]]:
    """Convenience: recent closed BTCUSDT candles for all Phase-1 TFs."""
    client = client or BinanceRestClient()
    result: dict[Timeframe, list[Candle]] = {}
    ingest = int(time.time() * 1000)
    for tf in TIMEFRAMES:
        rows = client.fetch_klines(symbol=DEFAULT_SYMBOL, timeframe=tf, limit=limit)
        # Drop the last row if it might still be forming; REST usually returns
        # only closed bars for completed intervals, but we mark all isFinal=True
        # per MD-1.2 (closed REST rows).
        result[tf] = [
            map_rest_kline(r, timeframe=tf, is_final=True, ingest_ts_ms=ingest) for r in rows
        ]
    return result
