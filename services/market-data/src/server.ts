/**
 * Offline-local market-data health server.

 * Local docker compose only – no VPS / host / SSH deploy paths.

 */
import http from 'node:http';
import { pathToFileURL } from 'node:url';
import type { Health } from './types/candle.js';

const PORT = Number(process.env.PORT ?? 8080);

function buildHealth(): Health {
  return {
    status: 'ok',
    lastSourceTsMs: Date.now(),
    venue: 'binance',
    symbol: 'BTCUSDT',
    activeTimeframes: ['1m', '5m', '15m', '1h', '4h', '1d', '1w'],
    note: 'scaffold health – ingest not started in this process',
  };
}

export function createServer(): http.Server {
  return http.createServer((req, res) => {
    if (req.method === 'GET' && (req.url === '/health' || req.url === '/health/')) {
      const body = JSON.stringify(buildHealth());
      res.writeHead(200, {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'no-store',
      });
      res.end(body);
      return;
    }
    res.writeHead(404, { 'content-type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({error: 'not_found'}));
  });
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
  const server = createServer();
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`market-data health listening on :${PORT} (local only)`);
  });
}
