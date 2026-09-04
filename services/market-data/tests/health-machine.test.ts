import { describe, expect, it } from 'vitest';
import {
  HealthMachine,
  STALE_THRESHOLD_MS,
} from '../src/health/index.js';
import { TIMEFRAMES } from '../src/types/candle.js';

describe('HealthMachine (MD-1.6)', () => {
  it('disconnected when feed is down', () => {
    let now = 1_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(false);
    hm.recordUpdate(now - 1_000, '1m');
    const h = hm.getHealth();
    expect(h.status).toBe('disconnected');
    expect(h.venue).toBe('binance');
    expect(h.symbol).toBe('BTCUSDT');
    expect(h.lastSourceTsMs).toBe(now - 1_000);
    expect(h.note).toMatch(/no socket/i);
  });

  it('stale when no update >60s despite connected feed', () => {
    let now = 10_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(true);
    hm.recordUpdate(now - STALE_THRESHOLD_MS - 1, '1m');
    hm.setActiveTimeframes([...TIMEFRAMES]);
    expect(hm.getHealth().status).toBe('stale');
    expect(hm.getHealth().note).toMatch(/>60s/);
  });

  it('stale when never received a source update', () => {
    const hm = new HealthMachine({ nowMs: () => 5_000 });
    hm.setFeedConnected(true);
    expect(hm.getHealth().status).toBe('stale');
    expect(hm.getHealth().lastSourceTsMs).toBe(0);
  });

  it('ok when connected, fresh, full TFs, no gaps', () => {
    const now = 20_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(true);
    hm.setActiveTimeframes([...TIMEFRAMES]);
    hm.recordUpdate(now - 5_000, '1m');
    const h = hm.getHealth();
    expect(h.status).toBe('ok');
    expect(h.lastSourceTsMs).toBe(now - 5_000);
    expect(h.activeTimeframes).toEqual([...TIMEFRAMES].sort());
    expect(h.gapCount).toBeUndefined();
  });

  it('degraded on partial TF failure (simple rule)', () => {
    const now = 30_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(true);
    hm.recordUpdate(now - 1_000, '1h');
    hm.setActiveTimeframes(['1h', '4h', '1d']);
    const h = hm.getHealth();
    expect(h.status).toBe('degraded');
    expect(h.activeTimeframes).toEqual(['1d', '1h', '4h']);
    expect(h.note).toMatch(/partial/i);
  });

  it('degraded when gapCount > 0 even with full TFs', () => {
    const now = 40_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(true);
    hm.setActiveTimeframes([...TIMEFRAMES]);
    hm.recordUpdate(now - 100, '1m');
    hm.setGapCount(3);
    const h = hm.getHealth();
    expect(h.status).toBe('degraded');
    expect(h.gapCount).toBe(3);
  });

  it('priority: disconnected beats stale and degraded', () => {
    const now = 50_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(false);
    hm.setGapCount(9);
    hm.recordUpdate(now - 120_000, '1m');
    expect(hm.getHealth().status).toBe('disconnected');
  });

  it('priority: stale beats degraded when age >60s', () => {
    const now = 60_000_000;
    const hm = new HealthMachine({ nowMs: () => now });
    hm.setFeedConnected(true);
    hm.setGapCount(2);
    hm.setActiveTimeframes(['1m']);
    hm.recordUpdate(now - 61_000, '1m');
    expect(hm.getHealth().status).toBe('stale');
  });

  it('transitions: disconnected → stale → degraded → ok', () => {
    let now = 100_000;
    const hm = new HealthMachine({ nowMs: () => now });

    expect(hm.getHealth().status).toBe('disconnected');

    hm.setFeedConnected(true);
    expect(hm.getHealth().status).toBe('stale');

    hm.recordUpdate(now - 1_000, '1m');
    hm.setActiveTimeframes(['1m', '5m']);
    hm.setGapCount(1);
    expect(hm.getHealth().status).toBe('degraded');

    hm.setGapCount(0);
    hm.setActiveTimeframes([...TIMEFRAMES]);
    expect(hm.getHealth().status).toBe('ok');

    // age past threshold → stale again
    now += STALE_THRESHOLD_MS + 1;
    expect(hm.getHealth().status).toBe('stale');
  });

  it('recordUpdate only advances lastSourceTsMs forward', () => {
    const hm = new HealthMachine({ nowMs: () => 1_000_000 });
    hm.setFeedConnected(true);
    hm.recordUpdate(500, '1m');
    hm.recordUpdate(400, '5m');
    expect(hm.getLastSourceTsMs()).toBe(500);
    hm.recordUpdate(800, '15m');
    expect(hm.getLastSourceTsMs()).toBe(800);
  });

  it('health object matches MD-1.1 field set', () => {
    const now = 70_000_000;
    const hm = new HealthMachine({
      venue: 'binance',
      symbol: 'BTCUSDT',
      nowMs: () => now,
    });
    hm.setFeedConnected(true);
    hm.setActiveTimeframes([...TIMEFRAMES]);
    hm.recordUpdate(now - 2_000);
    hm.setGapCount(0);
    hm.setNote('custom note');
    const h = hm.getHealth();
    expect(Object.keys(h).sort()).toEqual(
      ['activeTimeframes', 'lastSourceTsMs', 'note', 'status', 'symbol', 'venue'].sort(),
    );
    expect(['ok', 'degraded', 'stale', 'disconnected']).toContain(h.status);
  });
});
