"""MD-1.3 — Binance public WebSocket klines (forming + isFinal via k.x).

Reconnect/backoff skeleton. Integration tests are skipped by default
(set RUN_BINANCE_WS_INTEGRATION=1 to enable live checks).
"""

from __future__ import annotations

import json
import logging
import random
import threading
import time
from typing import Any, Callable, Iterable

from .candle import BINANCE_INTERVAL, TIMEFRAMES, Candle, Timeframe
from .errors import NetworkError

logger = logging.getLogger(__name__)

BINANCE_WS_BASE = "wss://stream.binance.com:9443"
DEFAULT_SYMBOL = "BTCUSDT"

# Backoff skeleton (seconds)
BACKOFF_INITIAL_S = 1.0
BACKOFF_MAX_S = 60.0
BACKOFF_MULTIPLIER = 2.0
BACKOFF_JITTER = 0.2


def combined_stream_url(
    timeframes: Iterable[Timeframe],
    *,
    symbol: str = DEFAULT_SYMBOL,
    base: str = BINANCE_WS_BASE,
) -> str:
    """Build combined stream URL for kline subscriptions."""
    sym = symbol.lower()
    streams = []
    for tf in timeframes:
        if tf not in TIMEFRAMES:
            raise ValueError(f"unsupported timeframe: {tf}")
        interval = BINANCE_INTERVAL[tf]
        streams.append(f"{sym}@kline_{interval}")
    joined = "/".join(streams)
    return f"{base}/stream?streams={joined}"


def map_ws_kline_event(
    payload: dict[str, Any],
    *,
    ingest_ts_ms: int | None = None,
    venue: str = "binance",
) -> Candle | None:
    """Map a Binance kline WS event (or combined-stream wrapper) to MD-1.1 Candle.

    Combined stream: {"stream": "...", "data": {"e":"kline","E":...,"s":"...","k":{...}}}
    Single stream: {"e":"kline","E":...,"s":"...","k":{...}}
    """
    data = payload.get("data", payload)
    if data.get("e") != "kline":
        return None
    k = data.get("k") or {}
    interval = k.get("i")
    if interval not in TIMEFRAMES:
        return None

    now = int(time.time() * 1000)
    event_ts = int(data.get("E") or now)
    is_final = bool(k.get("x", False))

    return Candle(
        venue=venue,  # type: ignore[arg-type]
        symbol=str(data.get("s") or k.get("s") or DEFAULT_SYMBOL),
        timeframe=interval,  # type: ignore[arg-type]
        openTimeMs=int(k["t"]),
        closeTimeMs=int(k["T"]),
        open=float(k["o"]),
        high=float(k["h"]),
        low=float(k["l"]),
        close=float(k["c"]),
        volume=float(k["v"]),
        isFinal=is_final,
        sourceTsMs=event_ts,
        ingestTsMs=ingest_ts_ms if ingest_ts_ms is not None else now,
    )


def next_backoff_s(attempt: int) -> float:
    """Exponential backoff with jitter. attempt starts at 0."""
    raw = min(BACKOFF_MAX_S, BACKOFF_INITIAL_S * (BACKOFF_MULTIPLIER ** attempt))
    jitter = raw * BACKOFF_JITTER * (2 * random.random() - 1)
    return max(0.1, raw + jitter)


CandleHandler = Callable[[Candle], None]


class BinanceKlineWsClient:
    """Public WS kline subscriber with reconnect/backoff skeleton.

    Uses the optional `websockets` package when running live. Unit tests
    exercise map_ws_kline_event without a network.
    """

    def __init__(
        self,
        *,
        symbol: str = DEFAULT_SYMBOL,
        timeframes: Iterable[Timeframe] = TIMEFRAMES,
        base_url: str = BINANCE_WS_BASE,
        on_candle: CandleHandler | None = None,
    ) -> None:
        self.symbol = symbol
        self.timeframes = tuple(timeframes)
        self.base_url = base_url
        self.on_candle = on_candle
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self._attempt = 0

    @property
    def url(self) -> str:
        return combined_stream_url(
            self.timeframes, symbol=self.symbol, base=self.base_url
        )

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._stop.clear()
        self._thread = threading.Thread(target=self._run_loop, name="binance-kline-ws", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=5.0)
            self._thread = None

    def _run_loop(self) -> None:
        """Reconnect loop with exponential backoff. Skeleton for Phase 1."""
        while not self._stop.is_set():
            try:
                self._connect_once()
                self._attempt = 0
            except Exception as exc:  # noqa: BLE001 — reconnect skeleton
                logger.warning("WS disconnected: %s", exc)
                if self._stop.is_set():
                    break
                delay = next_backoff_s(self._attempt)
                self._attempt += 1
                logger.info("WS reconnect in %.1fs (attempt %s)", delay, self._attempt)
                self._stop.wait(delay)

    def _connect_once(self) -> None:
        """Single connection session. Requires `websockets` at runtime."""
        try:
            import asyncio
            import websockets  # type: ignore[import-untyped]
        except ImportError as e:
            raise NetworkError(
                "websockets package required for live WS; pip install websockets"
            ) from e

        async def _session() -> None:
            async with websockets.connect(self.url, ping_interval=20) as ws:
                logger.info("WS connected %s", self.url)
                while not self._stop.is_set():
                    try:
                        raw = await asyncio.wait_for(ws.recv(), timeout=1.0)
                    except asyncio.TimeoutError:
                        continue
                    payload = json.loads(raw)
                    candle = map_ws_kline_event(payload)
                    if candle and self.on_candle:
                        self.on_candle(candle)

        asyncio.run(_session())
