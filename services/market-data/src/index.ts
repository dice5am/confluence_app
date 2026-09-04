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
export {
  createServer,
  createDefaultDeps,
  startLiveIngest,
} from './server.js';
export type { ServerDeps } from './server.js';
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
export { LiveCandleHub } from './live/index.js';
export type { LiveCandleHandler, LiveCandleHubOptions } from './live/index.js';
export {
  HISTORY_DEPTH_MS,
  earliestAllowedOpenTimeMs,
  clampHistoryFromMs,
  queryClosedHistory,
  parseTimeframe,
  parseVenue,
  HistoryQueryError,
  TIMEFRAME_MS,
  planBootstrapThenSubscribe,
  bootstrapThenSubscribe,
} from './api/index.js';
export type {
  HistoryQuery,
  HistoryResult,
  BootstrapSubscribePlan,
  BootstrapThenSubscribeResult,
  BootstrapFetchHistory,
  BootstrapSubscribe,
  CandleHandler,
  Unsubscribe,
} from './api/index.js';
