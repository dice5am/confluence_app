"""MD-1.1 candle contract types — field names locked."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any, Literal

Venue = Literal["binance", "bybit"]
Timeframe = Literal["1m", "5m", "15m", "1h", "4h", "1d", "1w"]

TIMEFRAMES: tuple[Timeframe, ...] = ("1m", "5m", "15m", "1h", "4h", "1d", "1w")

# Binance REST/WS interval strings match our TF enum for listed values.
BINANCE_INTERVAL: dict[Timeframe, str] = {tf: tf for tf in TIMEFRAMES}


@dataclass(frozen=True, slots=True)
class Candle:
    """Internal candle — primary key (venue, symbol, timeframe, openTimeMs)."""

    venue: Venue
    symbol: str
    timeframe: Timeframe
    openTimeMs: int
    closeTimeMs: int
    open: float
    high: float
    low: float
    close: float
    volume: float
    isFinal: bool
    sourceTsMs: int
    ingestTsMs: int

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True, slots=True)
class Health:
    """MD-1.1 §3 health object."""

    status: Literal["ok", "degraded", "stale", "disconnected"]
    lastSourceTsMs: int
    venue: str
    symbol: str
    gapCount: int | None = None
    activeTimeframes: list[str] | None = None
    note: str | None = None

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {
            "status": self.status,
            "lastSourceTsMs": self.lastSourceTsMs,
            "venue": self.venue,
            "symbol": self.symbol,
        }
        if self.gapCount is not None:
            d["gapCount"] = self.gapCount
        if self.activeTimeframes is not None:
            d["activeTimeframes"] = self.activeTimeframes
        if self.note is not None:
            d["note"] = self.note
        return d
