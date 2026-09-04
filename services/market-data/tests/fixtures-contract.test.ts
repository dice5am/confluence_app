/**
 * MD-1.12 — Offline golden/contract fixture tests.
 * Loads services/market-data/fixtures only; no network.
 */
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import type { Candle, Health, HealthStatus, Timeframe, Venue } from '../src/types/candle.js';
import { TIMEFRAMES } from '../src/types/candle.js';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures');
const venues: readonly Venue[] = ['binance', 'bybit'];
const healthStatuses: readonly HealthStatus[] = [
  'ok',
  'degraded',
  'stale',
  'disconnected',
];

/** MD-1.1 §1 required candle fields */
const REQUIRED_CANDLE_FIELDS = [
  'venue',
  'symbol',
  'timeframe',
  'openTimeMs',
  'closeTimeMs',
  'open',
  'high',
  'low',
  'close',
  'volume',
  'isFinal',
  'sourceTsMs',
  'ingestTsMs',
] as const;

function loadJson(path: string): unknown {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function assertFiniteNumber(value: unknown, label: string): void {
  expect(typeof value, label).toBe('number');
  expect(Number.isFinite(value as number), label).toBe(true);
}

function assertCandleFields(c: Candle, file: string, index: number): void {
  const label = `${file}[${index}]`;
  for (const field of REQUIRED_CANDLE_FIELDS) {
    expect(c, `${label} missing ${field}`).toHaveProperty(field);
  }
  expect(venues, label).toContain(c.venue);
  expect(c.symbol, label).toBeTypeOf('string');
  expect(c.symbol.length, label).toBeGreaterThan(0);
  expect(TIMEFRAMES, label).toContain(c.timeframe);
  for (const k of [
    'openTimeMs',
    'closeTimeMs',
    'open',
    'high',
    'low',
    'close',
    'volume',
    'sourceTsMs',
    'ingestTsMs',
  ] as const) {
    assertFiniteNumber(c[k], `${label}.${k}`);
  }
  expect(c.isFinal, `${label} isFinal`).toBe(true);
  expect(c.closeTimeMs, `${label} closeTimeMs`).toBeGreaterThanOrEqual(c.openTimeMs);
}

describe('MD-1.12 fixture contract (MD-1.1 fields)', () => {
  const candleFiles = readdirSync(join(root, 'candles'))
    .filter((f) => f.endsWith('-closed.json'))
    .sort();

  it('finds closed candle fixtures for all TFs', () => {
    expect(candleFiles).toEqual(
      expect.arrayContaining([
        'btcusdt-1m-closed.json',
        'btcusdt-5m-closed.json',
        'btcusdt-15m-closed.json',
        'btcusdt-1h-closed.json',
        'btcusdt-4h-closed.json',
        'btcusdt-1d-closed.json',
        'btcusdt-1w-closed.json',
      ]),
    );
  });

  for (const file of candleFiles) {
    it(`${file}: every candle has MD-1.1 required fields and isFinal=true`, () => {
      const doc = loadJson(join(root, 'candles', file)) as {
        contract?: string;
        candles: Candle[];
        venue: Venue;
        symbol: string;
        timeframe: Timeframe;
        count: number;
      };
      expect(doc.contract ?? 'MD-1.1').toBe('MD-1.1');
      expect(Array.isArray(doc.candles)).toBe(true);
      expect(doc.candles.length).toBeGreaterThan(0);
      expect(doc.count).toBe(doc.candles.length);
      expect(TIMEFRAMES).toContain(doc.timeframe);
      for (let i = 0; i < doc.candles.length; i++) {
        const c = doc.candles[i]!;
        assertCandleFields(c, file, i);
        expect(c.venue).toBe(doc.venue);
        expect(c.symbol).toBe(doc.symbol);
        expect(c.timeframe).toBe(doc.timeframe);
      }
    });
  }

  const healthFiles = readdirSync(join(root, 'health'))
    .filter((f) => f.endsWith('.json'))
    .sort();

  it('finds health fixtures covering every MD-1.1 enum value', () => {
    expect(healthFiles).toEqual(
      expect.arrayContaining([
        'ok.json',
        'degraded.json',
        'stale.json',
        'disconnected.json',
      ]),
    );
    const statuses = healthFiles.map((f) => {
      const h = loadJson(join(root, 'health', f)) as Health;
      return h.status;
    });
    expect(new Set(statuses)).toEqual(new Set(healthStatuses));
  });

  for (const file of healthFiles) {
    it(`${file}: health status is MD-1.1 enum`, () => {
      const h = loadJson(join(root, 'health', file)) as Health;
      expect(healthStatuses).toContain(h.status);
      expect(file.replace(/\.json$/, '')).toBe(h.status);
      assertFiniteNumber(h.lastSourceTsMs, `${file}.lastSourceTsMs`);
      expect(h.venue).toBeTypeOf('string');
      expect(h.symbol).toBeTypeOf('string');
      if (h.activeTimeframes !== undefined) {
        for (const tf of h.activeTimeframes) {
          expect(TIMEFRAMES).toContain(tf);
        }
      }
    });
  }
});
