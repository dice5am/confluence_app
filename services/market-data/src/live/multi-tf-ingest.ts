import { BinanceKlineWsClient, type BinanceKlineWsOptions } from '../binance/ws.js';
import type { HealthMachine } from '../health/machine.js';
import type { LiveCandleHub } from './hub.js';
import type { Timeframe } from '../types/candle.js';
import { resolveVenueNativeInterval } from '../policy/tf-policy.js';

export interface MultiTfLiveIngestOptions {
  hub: LiveCandleHub;
  health?: HealthMachine;
  symbol?: string;
  /**
   * TFs currently in use — only these get a WS subscription (MD-2.3).
   * Do not subscribe unused streams.
   */
  timeframes: readonly Timeframe[];
  /** Forwarded to each BinanceKlineWsClient (tests inject WebSocketImpl). */
  ws?: Omit<BinanceKlineWsOptions, 'symbol' | 'timeframe' | 'onCandle' | 'onStatus' | 'onError'>;
  onError?: (err: Error, timeframe: Timeframe) => void;
}

export interface MultiTfLiveIngestHandle {
  /** Currently subscribed TFs (copy). */
  subscribedTimeframes(): Timeframe[];
  /** Stop all streams and mark feed disconnected. */
  stop(): void;
  /** True after start() until stop(). */
  isRunning(): boolean;
}

/**
 * MD-2.3 — subscribe forming + final klines for a configurable in-use TF set.
 *
 * Each TF uses its venue-native Binance interval (MD-2.1). Candles upsert via
 * {@link LiveCandleHub} (store + exactly-one-final broadcast) and update
 * {@link HealthMachine} lastSource / activeTimeframes.
 */
export function startMultiTfLiveIngest(
  opts: MultiTfLiveIngestOptions,
): MultiTfLiveIngestHandle {
  if (!opts.timeframes || opts.timeframes.length === 0) {
    throw new Error('startMultiTfLiveIngest: timeframes must be non-empty');
  }

  // De-dupe while preserving order
  const seen = new Set<Timeframe>();
  const timeframes: Timeframe[] = [];
  for (const tf of opts.timeframes) {
    // Validate native interval exists (throws on unknown)
    resolveVenueNativeInterval(tf);
    if (!seen.has(tf)) {
      seen.add(tf);
      timeframes.push(tf);
    }
  }

  const symbol = opts.symbol ?? 'BTCUSDT';
  const clients: BinanceKlineWsClient[] = [];
  const openTfs = new Set<Timeframe>();
  let running = true;

  opts.health?.setFeedConnected(false);
  opts.health?.setNote('connecting multi-TF Binance public WS');
  opts.health?.setActiveTimeframes([]);
  // MD-2.9: expected = in-use set so subset subscriptions can reach `ok`
  opts.health?.setExpectedTimeframes(timeframes);

  const refreshHealthConnection = () => {
    if (!running) return;
    if (openTfs.size === 0) {
      opts.health?.setFeedConnected(false);
      opts.health?.setNote('live WS reconnecting / no streams open');
      return;
    }
    opts.health?.setFeedConnected(true);
    opts.health?.setActiveTimeframes([...openTfs].sort() as Timeframe[]);
    opts.health?.setNote(
      `live Binance public WS open for ${[...openTfs].sort().join(',')}`,
    );
  };

  for (const timeframe of timeframes) {
    const client = new BinanceKlineWsClient({
      ...opts.ws,
      symbol,
      timeframe,
      onCandle: (candle) => {
        // Hub upserts store + recordUpdate(lastSource) on HealthMachine
        opts.hub.ingest(candle);
      },
      onStatus: (status) => {
        if (status === 'open') {
          openTfs.add(timeframe);
          refreshHealthConnection();
        } else if (status === 'closed' || status === 'reconnecting') {
          openTfs.delete(timeframe);
          refreshHealthConnection();
        }
      },
      onError: (err) => {
        opts.health?.setNote(`live WS ${timeframe} error: ${err.message}`);
        opts.onError?.(err, timeframe);
      },
    });
    client.start();
    clients.push(client);
  }

  return {
    subscribedTimeframes: () => [...timeframes],
    isRunning: () => running,
    stop: () => {
      running = false;
      for (const c of clients) c.stop();
      openTfs.clear();
      opts.health?.setFeedConnected(false);
      opts.health?.setActiveTimeframes([]);
      opts.health?.setNote('multi-TF live ingest stopped');
    },
  };
}
