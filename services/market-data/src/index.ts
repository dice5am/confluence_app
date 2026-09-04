export type { Candle, Health, HealthStatus, Timeframe, Venue } from './types/candle.js';
export { TIMEFRAMES } from './types/candle.js';
export { BinanceApiError, classifyHttpStatus } from './binance/errors.js';
export {
  mapRestKlineToCandle,
  mapWsKlineToCandle,
  timeframeToBinanceInterval,
} from './binance/map.js';
export {
  fetchKlinesPage,
  fetchKlinesPaginated,
  fetchAllTimeframeKlines,
  BINANCE_REST_BASE,
} from './binance/rest.js';
export { BinanceKlineWsClient, BINANCE_WS_BASE } from './binance/ws.js';
export { createServer } from './server.js';
export { CandleStore } from './store/candle-store.js';
export type { CandleRangeQuery, CandleStoreOptions } from './store/candle-store.js';
export {
  ONE_MINUTE_MS,
  detectOneMinuteGaps,
  missingToRanges,
  fillOneMinuteGaps,
} from './gap/index.js';
export type {
  DetectGapsOptions,
  DetectGapsResult,
  GapRange,
  FetchKlinesFn,
  FillGapsOptions,
  FillGapsResult,
} from './gap/index.js';
export { HealthMachine, STALE_THRESHOLD_MS } from './health/index.js';
export type { HealthMachineOptions } from './health/index.js';
