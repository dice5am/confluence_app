import WebSocket from 'ws';
import type { Candle, Timeframe } from '../types/candle.js';
import { timeframeToBinanceInterval, mapWsKlineToCandle, type BinanceWsKlinePayload } from './map.js';
import {
  WS_INITIAL_BACKOFF_MS,
  WS_MAX_BACKOFF_MS,
  computeWsReconnectDelayMs,
} from './reconnect-policy.js';

export const BINANCE_WS_BASE = 'wss://stream.binance.com:9443';

export interface BinanceKlineWsOptions {
  symbol?: string;
  timeframe: Timeframe;
  wsBase?: string;
  /** Initial reconnect delay ms. */
  initialBackoffMs?: number;
  maxBackoffMs?: number;
  /** Injected for tests. */
  WebSocketImpl?: typeof WebSocket;
  nowMs?: () => number;
  onCandle?: (candle: Candle) => void;
  onError?: (err: Error) => void;
  onStatus?: (status: 'connecting' | 'open' | 'closed' | 'reconnecting') => void;
}

/**
 * Public Binance kline stream with reconnect/backoff skeleton.
 * No API key. Forming bars: isFinal=false; closed: isFinal=true (k.x).
 */
export class BinanceKlineWsClient {
  private ws: WebSocket | null = null;
  private stopped = false;
  private attempt = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly opts: Required<
    Pick<
      BinanceKlineWsOptions,
      'symbol' | 'timeframe' | 'wsBase' | 'initialBackoffMs' | 'maxBackoffMs'
    >
  > &
    BinanceKlineWsOptions;

  constructor(opts: BinanceKlineWsOptions) {
    this.opts = {
      ...opts,
      symbol: opts.symbol ?? 'BTCUSDT',
      timeframe: opts.timeframe,
      wsBase: opts.wsBase ?? BINANCE_WS_BASE,
      initialBackoffMs: opts.initialBackoffMs ?? WS_INITIAL_BACKOFF_MS,
      maxBackoffMs: opts.maxBackoffMs ?? WS_MAX_BACKOFF_MS,
    };
  }

  start(): void {
    this.stopped = false;
    this.connect();
  }

  stop(): void {
    this.stopped = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this.ws.removeAllListeners();
      this.ws.close();
      this.ws = null;
    }
    this.opts.onStatus?.('closed');
  }

  private streamUrl(): string {
    const symbol = this.opts.symbol.toLowerCase();
    const interval = timeframeToBinanceInterval(this.opts.timeframe);
    return `${this.opts.wsBase}/ws/${symbol}@kline_${interval}`;
  }

  private connect(): void {
    if (this.stopped) return;
    const WS = this.opts.WebSocketImpl ?? WebSocket;
    this.opts.onStatus?.('connecting');
    const ws = new WS(this.streamUrl());
    this.ws = ws;

    ws.on('open', () => {
      this.attempt = 0;
      this.opts.onStatus?.('open');
    });

    ws.on('message', (data) => {
      try {
        const raw = typeof data === 'string' ? data : data.toString('utf8');
        const payload = JSON.parse(raw) as BinanceWsKlinePayload;
        if (payload.e !== 'kline' || !payload.k) return;
        const candle = mapWsKlineToCandle(payload, {
          ingestTsMs: (this.opts.nowMs ?? Date.now)(),
        });
        this.opts.onCandle?.(candle);
      } catch (err) {
        this.opts.onError?.(err instanceof Error ? err : new Error(String(err)));
      }
    });

    ws.on('error', (err) => {
      this.opts.onError?.(err instanceof Error ? err : new Error(String(err)));
    });

    ws.on('close', () => {
      this.ws = null;
      if (this.stopped) {
        this.opts.onStatus?.('closed');
        return;
      }
      this.scheduleReconnect();
    });
  }

  /**
   * MD-2.8 exponential backoff with jitter — never tight-reconnect loops.
   * Uses shared {@link computeWsReconnectDelayMs}.
   */
  private scheduleReconnect(): void {
    this.opts.onStatus?.('reconnecting');
    const delay = computeWsReconnectDelayMs(this.attempt, {
      initialMs: this.opts.initialBackoffMs,
      maxMs: this.opts.maxBackoffMs,
    });
    this.attempt += 1;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);
  }
}

