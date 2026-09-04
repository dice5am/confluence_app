import Database from 'better-sqlite3';
import fs from 'node:fs';
import path from 'node:path';
import type { Candle, Timeframe, Venue } from '../types/candle.js';

export interface CandleRangeQuery {
  venue: Venue;
  symbol: string;
  timeframe: Timeframe;
  /** Inclusive lower bound on openTimeMs */
  fromMs: number;
  /** Inclusive upper bound on openTimeMs */
  toMs: number;
  /** When true, only return candles with isFinal=true */
  closedOnly?: boolean;
}

export interface CandleStoreOptions {
  /** SQLite file path. Defaults to data/candles.sqlite under cwd. */
  dbPath?: string;
}

const CREATE_SQL = `
CREATE TABLE IF NOT EXISTS candles (
  venue TEXT NOT NULL,
  symbol TEXT NOT NULL,
  timeframe TEXT NOT NULL,
  open_time_ms INTEGER NOT NULL,
  close_time_ms INTEGER NOT NULL,
  open REAL NOT NULL,
  high REAL NOT NULL,
  low REAL NOT NULL,
  close REAL NOT NULL,
  volume REAL NOT NULL,
  is_final INTEGER NOT NULL,
  source_ts_ms INTEGER NOT NULL,
  ingest_ts_ms INTEGER NOT NULL,
  PRIMARY KEY (venue, symbol, timeframe, open_time_ms)
);
CREATE INDEX IF NOT EXISTS idx_candles_range
  ON candles (venue, symbol, timeframe, open_time_ms);
`;

const UPSERT_SQL = `
INSERT INTO candles (
  venue, symbol, timeframe, open_time_ms, close_time_ms,
  open, high, low, close, volume, is_final, source_ts_ms, ingest_ts_ms
) VALUES (
  @venue, @symbol, @timeframe, @openTimeMs, @closeTimeMs,
  @open, @high, @low, @close, @volume, @isFinal, @sourceTsMs, @ingestTsMs
)
ON CONFLICT (venue, symbol, timeframe, open_time_ms) DO UPDATE SET
  close_time_ms = excluded.close_time_ms,
  open = excluded.open,
  high = excluded.high,
  low = excluded.low,
  close = excluded.close,
  volume = excluded.volume,
  is_final = excluded.is_final,
  source_ts_ms = excluded.source_ts_ms,
  ingest_ts_ms = excluded.ingest_ts_ms
`;

function rowToCandle(row: Record<string, unknown>): Candle {
  return {
    venue: row.venue as Venue,
    symbol: row.symbol as string,
    timeframe: row.timeframe as Timeframe,
    openTimeMs: Number(row.open_time_ms),
    closeTimeMs: Number(row.close_time_ms),
    open: Number(row.open),
    high: Number(row.high),
    low: Number(row.low),
    close: Number(row.close),
    volume: Number(row.volume),
    isFinal: Boolean(row.is_final),
    sourceTsMs: Number(row.source_ts_ms),
    ingestTsMs: Number(row.ingest_ts_ms),
  };
}

/**
 * MD-1.4 candle store: SQLite persistence keyed by
 * (venue, symbol, timeframe, openTimeMs).
 *
 * Idempotent upsert — re-writing the same key is OK; a forming bar may be
 * replaced by its final. Does not implement gap-fill, health, or HTTP APIs.
 */
export class CandleStore {
  private readonly db: Database.Database;
  readonly dbPath: string;

  constructor(options: CandleStoreOptions = {}) {
    this.dbPath =
      options.dbPath ?? path.join(process.cwd(), 'data', 'candles.sqlite');
    const dir = path.dirname(this.dbPath);
    fs.mkdirSync(dir, { recursive: true });
    this.db = new Database(this.dbPath);
    this.db.pragma('journal_mode = WAL');
    this.db.exec(CREATE_SQL);
  }

  /** Insert or replace a candle by primary key. */
  upsert(candle: Candle): void {
    this.db.prepare(UPSERT_SQL).run({
      venue: candle.venue,
      symbol: candle.symbol,
      timeframe: candle.timeframe,
      openTimeMs: candle.openTimeMs,
      closeTimeMs: candle.closeTimeMs,
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
      volume: candle.volume,
      isFinal: candle.isFinal ? 1 : 0,
      sourceTsMs: candle.sourceTsMs,
      ingestTsMs: candle.ingestTsMs,
    });
  }

  /** Upsert many candles in one transaction. */
  upsertMany(candles: readonly Candle[]): void {
    const stmt = this.db.prepare(UPSERT_SQL);
    const tx = this.db.transaction((rows: readonly Candle[]) => {
      for (const c of rows) {
        stmt.run({
          venue: c.venue,
          symbol: c.symbol,
          timeframe: c.timeframe,
          openTimeMs: c.openTimeMs,
          closeTimeMs: c.closeTimeMs,
          open: c.open,
          high: c.high,
          low: c.low,
          close: c.close,
          volume: c.volume,
          isFinal: c.isFinal ? 1 : 0,
          sourceTsMs: c.sourceTsMs,
          ingestTsMs: c.ingestTsMs,
        });
      }
    });
    tx(candles);
  }

  /**
   * Range query by venue/symbol/timeframe and openTimeMs bounds (inclusive).
   * Results ordered by openTimeMs ascending.
   */
  queryRange(q: CandleRangeQuery): Candle[] {
    const closedOnly = q.closedOnly === true;
    const sql = closedOnly
      ? `SELECT * FROM candles
         WHERE venue = @venue AND symbol = @symbol AND timeframe = @timeframe
           AND open_time_ms >= @fromMs AND open_time_ms <= @toMs
           AND is_final = 1
         ORDER BY open_time_ms ASC`
      : `SELECT * FROM candles
         WHERE venue = @venue AND symbol = @symbol AND timeframe = @timeframe
           AND open_time_ms >= @fromMs AND open_time_ms <= @toMs
         ORDER BY open_time_ms ASC`;

    const rows = this.db.prepare(sql).all({
      venue: q.venue,
      symbol: q.symbol,
      timeframe: q.timeframe,
      fromMs: q.fromMs,
      toMs: q.toMs,
    }) as Record<string, unknown>[];

    return rows.map(rowToCandle);
  }

  /** Fetch a single candle by primary key, or undefined. */
  get(
    venue: Venue,
    symbol: string,
    timeframe: Timeframe,
    openTimeMs: number,
  ): Candle | undefined {
    const row = this.db
      .prepare(
        `SELECT * FROM candles
         WHERE venue = ? AND symbol = ? AND timeframe = ? AND open_time_ms = ?`,
      )
      .get(venue, symbol, timeframe, openTimeMs) as
      | Record<string, unknown>
      | undefined;
    return row ? rowToCandle(row) : undefined;
  }

  /** Row count (for tests / diagnostics). */
  count(): number {
    const row = this.db.prepare('SELECT COUNT(*) AS n FROM candles').get() as {
      n: number;
    };
    return row.n;
  }

  close(): void {
    this.db.close();
  }
}
