import { describe, expect, it } from 'vitest';
import { classifyHttpStatus } from '../src/binance/errors.js';

describe('classifyHttpStatus', () => {
  it('maps known status codes', () => {
    expect(classifyHttpStatus(429).code).toBe('RATE_LIMIT_429');
    expect(classifyHttpStatus(418).code).toBe('IP_BAN_418');
    expect(classifyHttpStatus(500).code).toBe('SERVER_5XX');
    expect(classifyHttpStatus(400).code).toBe('UNKNOWN');
  });
});
