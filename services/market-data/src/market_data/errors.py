"""Typed errors for Binance public REST/WS (no signed endpoints)."""

from __future__ import annotations


class MarketDataError(Exception):
    """Base market-data error."""


class RateLimitError(MarketDataError):
    """HTTP 429 — too many requests; honor Retry-After when present."""

    def __init__(self, message: str = "rate limited", *, retry_after_s: float | None = None):
        super().__init__(message)
        self.retry_after_s = retry_after_s


class BanError(MarketDataError):
    """HTTP 418 — IP banned (Binance WAF / weight)."""

    def __init__(self, message: str = "IP banned (418)"):
        super().__init__(message)


class ServerError(MarketDataError):
    """HTTP 5xx from venue."""

    def __init__(self, message: str, *, status_code: int):
        super().__init__(message)
        self.status_code = status_code


class TimeoutError(MarketDataError):
    """Request timed out."""

    def __init__(self, message: str = "request timed out"):
        super().__init__(message)


class NetworkError(MarketDataError):
    """DNS / connection / other transport failure."""

    def __init__(self, message: str = "network error"):
        super().__init__(message)
