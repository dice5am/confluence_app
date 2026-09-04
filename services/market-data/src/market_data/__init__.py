"""Market data service — Phase 1 Binance scaffold (MD-1.1 / MD-1.2 / MD-1.3 / MD-1.10)."""

from .candle import Candle, TIMEFRAMES, Venue, Timeframe
from .errors import (
    MarketDataError,
    RateLimitError,
    BanError,
    ServerError,
    TimeoutError,
    NetworkError,
)

__all__ = [
    "Candle",
    "TIMEFRAMES",
    "Venue",
    "Timeframe",
    "MarketDataError",
    "RateLimitError",
    "BanError",
    "ServerError",
    "TimeoutError",
    "NetworkError",
]
