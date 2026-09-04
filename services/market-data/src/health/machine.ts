import type { Health, HealthStatus, Timeframe, Venue } from '../types/candle.js';
import { TIMEFRAMES } from '../types/candle.js';

/** MD-1.1 / MD-1.6: no update for more than 60s → stale */
export const STALE_THRESHOLD_MS = 60_000;

export interface HealthMachineOptions {
  venue?: Venue | string;
  symbol?: string;
  /** Expected TFs for partial-failure degraded rule. Defaults to all Phase-1 TFs. */
  expectedTimeframes?: readonly Timeframe[];
  /** Clock injection for tests. */
  nowMs?: () => number;
}

/**
 * MD-1.6 health state machine.
 *
 * Priority (highest first):
 * 1. `disconnected` — WS/feed explicitly down
 * 2. `stale` — no source update for >60s (or never received)
 * 3. `degraded` — partial TF coverage and/or gapCount > 0
 * 4. `ok` — feed up, fresh updates, full TF set, no gaps
 */
export class HealthMachine {
  private readonly venue: string;
  private readonly symbol: string;
  private expectedTimeframes: readonly Timeframe[];
  private readonly nowMs: () => number;

  private lastSourceTsMs = 0;
  private feedConnected = false;
  private activeTimeframes = new Set<Timeframe>();
  private gapCount = 0;
  private note: string | undefined;

  constructor(options: HealthMachineOptions = {}) {
    this.venue = options.venue ?? 'binance';
    this.symbol = options.symbol ?? 'BTCUSDT';
    this.expectedTimeframes = options.expectedTimeframes ?? TIMEFRAMES;
    this.nowMs = options.nowMs ?? Date.now;
  }

  /** Record a live/REST source observation. Updates lastSourceTsMs. */
  recordUpdate(sourceTsMs: number, timeframe?: Timeframe): void {
    if (sourceTsMs > this.lastSourceTsMs) {
      this.lastSourceTsMs = sourceTsMs;
    }
    if (timeframe) {
      this.activeTimeframes.add(timeframe);
    }
  }

  /** WS / feed connectivity. false → disconnected. */
  setFeedConnected(connected: boolean): void {
    this.feedConnected = connected;
  }

  /** Remaining or recently detected 1m gap count (from MD-1.5). */
  setGapCount(gapCount: number): void {
    this.gapCount = Math.max(0, Math.floor(gapCount));
  }

  /** Replace the set of TFs currently receiving updates. */
  setActiveTimeframes(timeframes: readonly Timeframe[]): void {
    this.activeTimeframes = new Set(timeframes);
  }

  /**
   * MD-2.9: set the TF set we expect to be live (in-use).
   * Multi-TF ingest should set this to the subscribed set so a chart
   * subset is not permanently `degraded` vs all 7 product TFs.
   */
  setExpectedTimeframes(timeframes: readonly Timeframe[]): void {
    this.expectedTimeframes = [...timeframes];
  }

  getExpectedTimeframes(): Timeframe[] {
    return [...this.expectedTimeframes];
  }

  /** Optional operator note attached to the health object. */
  setNote(note: string | undefined): void {
    this.note = note;
  }

  getLastSourceTsMs(): number {
    return this.lastSourceTsMs;
  }

  getGapCount(): number {
    return this.gapCount;
  }

  isFeedConnected(): boolean {
    return this.feedConnected;
  }

  /** Compute MD-1.1 Health snapshot. */
  getHealth(nowMs?: number): Health {
    const now = nowMs ?? this.nowMs();
    const active = [...this.activeTimeframes].sort();
    const status = this.resolveStatus(now);
    const health: Health = {
      status,
      lastSourceTsMs: this.lastSourceTsMs,
      venue: this.venue,
      symbol: this.symbol,
    };

    if (this.gapCount > 0) {
      health.gapCount = this.gapCount;
    }
    if (active.length > 0) {
      health.activeTimeframes = active as Timeframe[];
    }

    const note = this.note ?? defaultNote(status);
    if (note) health.note = note;

    return health;
  }

  private resolveStatus(nowMs: number): HealthStatus {
    if (!this.feedConnected) {
      return 'disconnected';
    }

    const age =
      this.lastSourceTsMs <= 0 ? Number.POSITIVE_INFINITY : nowMs - this.lastSourceTsMs;
    if (age > STALE_THRESHOLD_MS) {
      return 'stale';
    }

    const partialTf =
      this.activeTimeframes.size > 0 &&
      this.activeTimeframes.size < this.expectedTimeframes.length;
    if (partialTf || this.gapCount > 0) {
      return 'degraded';
    }

    return 'ok';
  }
}

function defaultNote(status: HealthStatus): string | undefined {
  switch (status) {
    case 'ok':
      return 'live updates flowing';
    case 'degraded':
      return 'partial TF coverage / gaps present';
    case 'stale':
      return 'no update >60s — Alerts MUST suppress new confluence fires';
    case 'disconnected':
      return 'no socket / no feed — Alerts MUST suppress';
    default:
      return undefined;
  }
}
