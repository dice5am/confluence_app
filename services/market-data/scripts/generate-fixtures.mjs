#!/usr/bin/env node
/**
 * Generate offline MD-1.1 candle + health fixtures for Alerts golden vectors.
 * Timestamps are synthetic; calendar anchors use America/Toronto.
 *
 * Usage: node scripts/generate-fixtures.mjs
 */
import { mkdirSync, writeFileSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, '..');
const candlesDir = join(root, 'fixtures', 'candles');
const healthDir = join(root, 'fixtures', 'health');

mkdirSync(candlesDir, { recursive: true });
mkdirSync(healthDir, { recursive: true });

/** 2026-01-01T00:00:00 America/Toronto = 2026-01-01T05:00:00.000Z (EST, UTC-5) */
const YTD_ANCHOR_OPEN_MS = Date.parse('2026-01-01T05:00:00.000Z');

/** End of synthetic series: 2026-09-04T12:00:00 America/Toronto = 2026-09-04T16:00:00.000Z (EDT, UTC-4) */
const SERIES_END_OPEN_MS = Date.parse('2026-09-04T16:00:00.000Z');

const TF_MS = {
  '1m': 60_000,
  '5m': 5 * 60_000,
  '15m': 15 * 60_000,
  '1h': 60 * 60_000,
  '4h': 4 * 60 * 60_000,
  '1d': 24 * 60 * 60_000,
  '1w': 7 * 24 * 60 * 60_000,
};

const COUNTS = {
  '1m': 120,
  '5m': 120,
  '15m': 120,
  // Enough for SMA-200 (>=220) and full YTD VWAP span on 1h
  '1h': null, // computed from YTD span
  '4h': 120,
  '1d': 90,
  '1w': 60,
};

function round(n, d = 2) {
  const f = 10 ** d;
  return Math.round(n * f) / f;
}

function makeCandle(tf, openTimeMs, i) {
  const ms = TF_MS[tf];
  const closeTimeMs = openTimeMs + ms - 1;
  // Deterministic walk around ~42k
  const base = 42000 + Math.sin(i / 17) * 80 + Math.cos(i / 41) * 40;
  const open = round(base);
  const close = round(base + Math.sin(i / 7) * 25);
  const high = round(Math.max(open, close) + 10 + (i % 7));
  const low = round(Math.min(open, close) - 15 - (i % 5));
  const volume = round(10 + (i % 50) * 0.1 + Math.abs(Math.sin(i / 13)) * 5, 2);
  return {
    venue: 'binance',
    symbol: 'BTCUSDT',
    timeframe: tf,
    openTimeMs,
    closeTimeMs,
    open,
    high,
    low,
    close,
    volume,
    isFinal: true,
    sourceTsMs: closeTimeMs,
    ingestTsMs: closeTimeMs + 50,
  };
}

function buildSeries(tf, count, endOpenMs) {
  const ms = TF_MS[tf];
  const firstOpen = endOpenMs - (count - 1) * ms;
  const candles = [];
  for (let i = 0; i < count; i++) {
    candles.push(makeCandle(tf, firstOpen + i * ms, i));
  }
  return candles;
}

function writeJson(path, obj) {
  const text = JSON.stringify(obj, null, 2) + '\n';
  writeFileSync(path, text, 'utf8');
  // Verify parse
  JSON.parse(readFileSync(path, 'utf8'));
  return path;
}

const timezoneNote =
  'Synthetic openTimes aligned for America/Toronto calendar anchors. ' +
  'YTD start for 2026 = 2026-01-01T00:00:00 America/Toronto = 2026-01-01T05:00:00.000Z (EST, UTC-5). ' +
  'Series end open ≈ 2026-09-04T12:00:00 America/Toronto = 2026-09-04T16:00:00.000Z (EDT, UTC-4). ' +
  'Timestamps are synthetic/offline — not live Binance history.';

// 1h: from YTD anchor through SERIES_END (inclusive), ensure >=220
const hourMs = TF_MS['1h'];
let h1Count = Math.floor((SERIES_END_OPEN_MS - YTD_ANCHOR_OPEN_MS) / hourMs) + 1;
if (h1Count < 220) h1Count = 220;
const h1Candles = buildSeries('1h', h1Count, SERIES_END_OPEN_MS);
const pitIdx = 100;
const pitAnchor = h1Candles[pitIdx].openTimeMs;

writeJson(join(candlesDir, 'btcusdt-1h-closed.json'), {
  timezoneNote,
  contract: 'MD-1.1',
  venue: 'binance',
  symbol: 'BTCUSDT',
  timeframe: '1h',
  count: h1Candles.length,
  ytdAnchorOpenTimeMs: YTD_ANCHOR_OPEN_MS,
  pitCustomAnchorOpenTimeMs: pitAnchor,
  pitCustomAnchorNote: `bar index ${pitIdx} openTime for GV-VWAP-PIT`,
  candles: h1Candles,
});

for (const tf of ['1m', '5m', '15m', '4h', '1d', '1w']) {
  const count = COUNTS[tf];
  const candles = buildSeries(tf, count, SERIES_END_OPEN_MS);
  writeJson(join(candlesDir, `btcusdt-${tf}-closed.json`), {
    timezoneNote,
    contract: 'MD-1.1',
    venue: 'binance',
    symbol: 'BTCUSDT',
    timeframe: tf,
    count: candles.length,
    candles,
  });
}

const lastSource = SERIES_END_OPEN_MS + hourMs; // just after last 1h close window start+1h
writeJson(join(healthDir, 'ok.json'), {
  status: 'ok',
  lastSourceTsMs: lastSource,
  venue: 'binance',
  symbol: 'BTCUSDT',
  activeTimeframes: ['1m', '5m', '15m', '1h', '4h', '1d', '1w'],
  note: 'live updates flowing',
});
writeJson(join(healthDir, 'degraded.json'), {
  status: 'degraded',
  lastSourceTsMs: lastSource - 15_000,
  venue: 'binance',
  symbol: 'BTCUSDT',
  gapCount: 2,
  activeTimeframes: ['1h', '4h', '1d'],
  note: 'partial TF coverage / failover in progress',
});
writeJson(join(healthDir, 'stale.json'), {
  status: 'stale',
  lastSourceTsMs: lastSource - 90_000,
  venue: 'binance',
  symbol: 'BTCUSDT',
  note: 'no update >60s — Alerts MUST suppress new confluence fires',
});
writeJson(join(healthDir, 'disconnected.json'), {
  status: 'disconnected',
  lastSourceTsMs: lastSource - 600_000,
  venue: 'binance',
  symbol: 'BTCUSDT',
  note: 'no socket / no feed — Alerts MUST suppress',
});

console.log('Generated fixtures:');
console.log(`  1h bars: ${h1Candles.length} (ytdAnchor=${YTD_ANCHOR_OPEN_MS}, endOpen=${SERIES_END_OPEN_MS})`);
for (const tf of Object.keys(TF_MS)) {
  const p = join(candlesDir, `btcusdt-${tf}-closed.json`);
  const d = JSON.parse(readFileSync(p, 'utf8'));
  console.log(`  ${tf}: ${d.candles.length} OK`);
}
for (const h of ['ok', 'degraded', 'stale', 'disconnected']) {
  JSON.parse(readFileSync(join(healthDir, `${h}.json`), 'utf8'));
  console.log(`  health/${h}.json OK`);
}
