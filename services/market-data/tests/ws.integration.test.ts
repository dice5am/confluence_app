import { describe, expect, it } from 'vitest';
import { BinanceKlineWsClient } from '../src/binance/ws.js';
import type { Candle } from '../src/types/candle.js';

const run = process.env.RUN_WS_INTEGRATION === '1';

describe.skipIf(!run)('Binance WS integration (live network)', () => {
  it('receives at least one kline event', async () => {
    const candles: Candle[] = [];
    const client = new BinanceKlineWsClient({
      timeframe: '1m',
      onCandle: (c) => candles.push(c),
    });
    client.start();
    await new Promise<void>((resolve, reject) => {
      const t = setTimeout(() => reject(new Error('timeout waiting for WS kline')), 20_000);
      const iv = setInterval(() => {
        if (candles.length > 0) {
          clearInterval(iv);
          clearTimeout(t);
          resolve();
        }
      }, 200);
    });
    client.stop();
    expect(candles[0]!.venue).toBe('binance');
    expect(candles[0]!.symbol).toBe('BTCUSDT');
    expect(typeof candles[0]!.isFinal).toBe('boolean');
  }, 25_000);
});

describe('BinanceKlineWsClient skeleton', () => {
  it('exports client class', () => {
    expect(typeof BinanceKlineWsClient).toBe('function');
  });
});
