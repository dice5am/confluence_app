import type { CandleStore } from '../store/candle-store.js';
import type { HealthMachine } from '../health/machine.js';
import type { Candle } from '../types/candle.js';

export type LiveCandleHandler = (candle: Candle) => void;

export interface LiveCandleHubOptions {
  store: CandleStore;
  health?: HealthMachine;
  /**
   * When true (default), forming bars (isFinal=false) are upserted to the store.
   * Finals always upsert.
   */
  persistForming?: boolean;
}

function barKey(c: Candle): string {
  return `${c.venue}|${c.symbol}|${c.timeframe}|${c.openTimeMs}`;
}

/**
 * MD-1.8 live hub: wire Binance WS (or any source) → CandleStore + subscribers.
 *
 * - Forming updates: `isFinal=false` pushed to all subscribers
 * - Closed bars: **exactly one** `isFinal=true` push per
 *   `(venue,symbol,timeframe,openTimeMs)` — subsequent finals for the same
 *   key still upsert the store (idempotent) but are NOT re-broadcast
 *
 * **Alerts path:** consume finals only (`isFinal===true`). Charts may paint
 * forming. This hub does not filter; consumers choose.
 */
export class LiveCandleHub {
  private readonly store: CandleStore;
  private readonly health: HealthMachine | undefined;
  private readonly persistForming: boolean;
  private readonly subscribers = new Set<LiveCandleHandler>();
  /** Bar keys for which a final has already been broadcast. */
  private readonly finalsBroadcast = new Set<string>();

  constructor(options: LiveCandleHubOptions) {
    this.store = options.store;
    this.health = options.health;
    this.persistForming = options.persistForming !== false;
  }

  /** Subscribe to live candle events. Returns unsubscribe. */
  subscribe(handler: LiveCandleHandler): () => void {
    this.subscribers.add(handler);
    return () => {
      this.subscribers.delete(handler);
    };
  }

  subscriberCount(): number {
    return this.subscribers.size;
  }

  /**
   * Ingest a mapped candle (e.g. from BinanceKlineWsClient.onCandle).
   * Upserts store, updates health, broadcasts per exactly-one-final rule.
   */
  ingest(candle: Candle): { broadcast: boolean; storeUpsert: boolean } {
    const key = barKey(candle);
    let storeUpsert = false;
    let broadcast = false;

    if (candle.isFinal || this.persistForming) {
      this.store.upsert(candle);
      storeUpsert = true;
    }

    this.health?.recordUpdate(candle.sourceTsMs, candle.timeframe);

    if (candle.isFinal) {
      if (this.finalsBroadcast.has(key)) {
        return { broadcast: false, storeUpsert };
      }
      this.finalsBroadcast.add(key);
      broadcast = true;
    } else {
      broadcast = true;
    }

    if (broadcast) {
      for (const handler of this.subscribers) {
        try {
          handler(candle);
        } catch {
          /* subscriber errors must not break ingest */
        }
      }
    }

    return { broadcast, storeUpsert };
  }

  /** Test/diagnostics: has a final already been pushed for this bar? */
  hasBroadcastFinal(
    venue: string,
    symbol: string,
    timeframe: string,
    openTimeMs: number,
  ): boolean {
    return this.finalsBroadcast.has(
      `${venue}|${symbol}|${timeframe}|${openTimeMs}`,
    );
  }

  /** Clear final-broadcast bookkeeping (tests / venue switch). */
  resetFinalTracking(): void {
    this.finalsBroadcast.clear();
  }
}
