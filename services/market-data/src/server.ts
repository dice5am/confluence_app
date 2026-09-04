/**
 * Market-data HTTP consumer APIs (MD-1.7 / 1.8 / 1.9) + health.
 * Phase 2: multi-TF live ingest via startMultiTfLiveIngest (MD-2.3).
 *
 * Local docker compose only – no VPS / host / SSH deploy paths.
 *
 * Endpoints:
 * - GET /health | /v1/health     → HealthMachine snapshot
 * - GET /v1/candles              → closed history (MD-1.7)
 * - GET /v1/candles/stream       → SSE live forming + finals (MD-1.8)
 * - GET /v1/bootstrap-plan       → MD-1.9 sequence helper for Mobile/Alerts
 */
import http from 'node:http';
import { pathToFileURL } from 'node:url';
import { URL } from 'node:url';
import { CandleStore } from './store/candle-store.js';
import { HealthMachine } from './health/machine.js';
import { LiveCandleHub } from './live/hub.js';
import {
  HistoryQueryError,
  parseTimeframe,
  queryClosedHistory,
  planBootstrapThenSubscribe,
} from './api/index.js';
import type { Timeframe } from './types/candle.js';
import { startMultiTfLiveIngest } from './live/multi-tf-ingest.js';
import { TIMEFRAMES } from './types/candle.js';

const PORT = Number(process.env.PORT ?? 8080);

export interface ServerDeps {
  store: CandleStore;
  health: HealthMachine;
  hub: LiveCandleHub;
  nowMs?: () => number;
}

export function createDefaultDeps(options?: {
  dbPath?: string;
  nowMs?: () => number;
}): ServerDeps {
  const nowMs = options?.nowMs ?? Date.now;
  const store = new CandleStore(
    options?.dbPath !== undefined ? { dbPath: options.dbPath } : {},
  );
  const health = new HealthMachine({
    venue: 'binance',
    symbol: 'BTCUSDT',
    nowMs,
  });
  // Feed starts disconnected until live ingest connects (honest health).
  health.setFeedConnected(false);
  health.setNote('consumer APIs ready — live ingest not connected');
  const hub = new LiveCandleHub({ store, health });
  return { store, health, hub, nowMs };
}

function json(
  res: http.ServerResponse,
  status: number,
  body: unknown,
  extraHeaders?: Record<string, string>,
): void {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    ...extraHeaders,
  });
  res.end(payload);
}

function parseUrl(req: http.IncomingMessage): URL {
  return new URL(req.url ?? '/', `http://${req.headers.host ?? 'localhost'}`);
}

function requireNumber(
  params: URLSearchParams,
  name: string,
): number | { error: string } {
  const raw = params.get(name);
  if (raw === null || raw === '') {
    return { error: `missing query param: ${name}` };
  }
  const n = Number(raw);
  if (!Number.isFinite(n)) {
    return { error: `invalid number for ${name}` };
  }
  return n;
}

export function createServer(deps?: ServerDeps): http.Server {
  const d = deps ?? createDefaultDeps();
  const nowMs = d.nowMs ?? Date.now;

  return http.createServer((req, res) => {
    if (req.method !== 'GET') {
      json(res, 405, { error: 'method_not_allowed' });
      return;
    }

    const url = parseUrl(req);
    const path = url.pathname.replace(/\/+$/, '') || '/';

    // --- Health (MD-1.6 exposed) ---
    if (path === '/health' || path === '/v1/health') {
      json(res, 200, d.health.getHealth(nowMs()));
      return;
    }

    // --- MD-1.7 History GET ---
    if (path === '/v1/candles') {
      handleHistory(req, res, d, url, nowMs);
      return;
    }

    // --- MD-1.8 Live SSE ---
    if (path === '/v1/candles/stream') {
      handleLiveStream(req, res, d, url);
      return;
    }

    // --- MD-1.9 Bootstrap plan helper ---
    if (path === '/v1/bootstrap-plan') {
      handleBootstrapPlan(res, url, nowMs);
      return;
    }

    json(res, 404, { error: 'not_found' });
  });
}

function handleHistory(
  _req: http.IncomingMessage,
  res: http.ServerResponse,
  d: ServerDeps,
  url: URL,
  nowMs: () => number,
): void {
  const fromMs = requireNumber(url.searchParams, 'fromMs');
  if (typeof fromMs === 'object') {
    json(res, 400, { error: 'bad_request', message: fromMs.error });
    return;
  }
  const toMs = requireNumber(url.searchParams, 'toMs');
  if (typeof toMs === 'object') {
    json(res, 400, { error: 'bad_request', message: toMs.error });
    return;
  }

  const timeframeRaw = url.searchParams.get('timeframe') ?? '';
  const symbol = url.searchParams.get('symbol') ?? 'BTCUSDT';
  const venue = url.searchParams.get('venue') ?? 'binance';

  try {
    const result = queryClosedHistory(
      d.store,
      { venue, symbol, timeframe: timeframeRaw, fromMs, toMs },
      nowMs(),
    );
    json(res, 200, {
      venue: result.venue,
      symbol: result.symbol,
      timeframe: result.timeframe,
      fromMs: result.fromMs,
      toMs: result.toMs,
      effectiveFromMs: result.effectiveFromMs,
      truncated: result.truncated,
      candles: result.candles,
    });
  } catch (err) {
    if (err instanceof HistoryQueryError) {
      json(res, 400, { error: err.code, message: err.message });
      return;
    }
    json(res, 500, { error: 'internal_error' });
  }
}

/**
 * SSE stream of candle events.
 *
 * Query: symbol, timeframe (optional filter), finalsOnly=1 (Alerts path).
 * Each event: `event: candle\ndata: <Candle JSON>\n\n`
 *
 * Alerts: pass `finalsOnly=1` (or filter client-side for isFinal===true).
 */
function handleLiveStream(
  req: http.IncomingMessage,
  res: http.ServerResponse,
  d: ServerDeps,
  url: URL,
): void {
  const symbolFilter = url.searchParams.get('symbol') ?? undefined;
  const tfRaw = url.searchParams.get('timeframe');
  const tfFilter = tfRaw ? parseTimeframe(tfRaw) : null;
  if (tfRaw && !tfFilter) {
    json(res, 400, {
      error: 'invalid_timeframe',
      message: `timeframe must be one of ${TIMEFRAMES.join('|')}`,
    });
    return;
  }
  const finalsOnly =
    url.searchParams.get('finalsOnly') === '1' ||
    url.searchParams.get('finalsOnly') === 'true';

  res.writeHead(200, {
    'content-type': 'text/event-stream; charset=utf-8',
    'cache-control': 'no-cache, no-store',
    connection: 'keep-alive',
    'x-accel-buffering': 'no',
  });
  res.write(': ok\n\n');

  const send = (event: string, data: unknown) => {
    res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
  };

  // Snapshot health on connect
  send('health', d.health.getHealth());

  const unsubscribe = d.hub.subscribe((candle) => {
    if (symbolFilter && candle.symbol !== symbolFilter) return;
    if (tfFilter && candle.timeframe !== tfFilter) return;
    if (finalsOnly && !candle.isFinal) return;
    send('candle', candle);
  });

  const keepalive = setInterval(() => {
    res.write(': keepalive\n\n');
  }, 15_000);

  const cleanup = () => {
    clearInterval(keepalive);
    unsubscribe();
  };

  req.on('close', cleanup);
  res.on('close', cleanup);
}

function handleBootstrapPlan(
  res: http.ServerResponse,
  url: URL,
  nowMs: () => number,
): void {
  const tfRaw = url.searchParams.get('timeframe') ?? '1m';
  const timeframe = parseTimeframe(tfRaw);
  if (!timeframe) {
    json(res, 400, {
      error: 'invalid_timeframe',
      message: `timeframe must be one of ${TIMEFRAMES.join('|')}`,
    });
    return;
  }
  const lookbackMs = Number(url.searchParams.get('lookbackMs') ?? 60 * 60_000);
  if (!Number.isFinite(lookbackMs) || lookbackMs <= 0) {
    json(res, 400, {
      error: 'invalid_lookback',
      message: 'lookbackMs must be a positive number',
    });
    return;
  }
  const plan = planBootstrapThenSubscribe({
    timeframe,
    lookbackMs,
    nowMs: nowMs(),
  });
  json(res, 200, {
    timeframe,
    lookbackMs,
    ...plan,
    endpoints: {
      history: 'GET /v1/candles?symbol=&timeframe=&fromMs=&toMs=&venue=',
      live: 'GET /v1/candles/stream?symbol=&timeframe=&finalsOnly=',
      health: 'GET /v1/health',
    },
    alertsNote:
      'Alerts MUST consume isFinal=true only (use finalsOnly=1 on the stream).',
  });
}

/** Optional live Binance WS → hub ingest (local/box only; no secrets). MD-2.3 multi-TF. */
export function startLiveIngest(
  deps: ServerDeps,
  options?: { timeframes?: readonly Timeframe[]; symbol?: string },
): { stop: () => void; subscribedTimeframes: () => Timeframe[] } {
  const timeframes = options?.timeframes ?? TIMEFRAMES;
  const handle = startMultiTfLiveIngest({
    hub: deps.hub,
    health: deps.health,
    symbol: options?.symbol ?? 'BTCUSDT',
    timeframes,
  });
  return {
    stop: () => handle.stop(),
    subscribedTimeframes: () => handle.subscribedTimeframes(),
  };
}

function isMainModule(): boolean {
  const entry = process.argv[1];
  if (!entry) return false;
  try {
    return import.meta.url === pathToFileURL(entry).href;
  } catch {
    return false;
  }
}

if (isMainModule()) {
  const deps = createDefaultDeps();
  const server = createServer(deps);
  let ingestStop: (() => void) | undefined;

  if (process.env.ENABLE_LIVE_INGEST === '1') {
    const raw = process.env.LIVE_TIMEFRAMES?.trim();
    const tfs = (raw
      ? raw.split(',').map((s) => s.trim()).filter(Boolean)
      : [...TIMEFRAMES]) as Timeframe[];
    const handle = startLiveIngest(deps, { timeframes: tfs });
    ingestStop = handle.stop;
  }

  server.listen(PORT, '0.0.0.0', () => {
    console.log(
      `market-data listening on :${PORT} (local only)` +
        (ingestStop ? ' [live ingest on]' : ' [live ingest off]'),
    );
  });

  const shutdown = () => {
    ingestStop?.();
    deps.store.close();
    server.close();
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}
