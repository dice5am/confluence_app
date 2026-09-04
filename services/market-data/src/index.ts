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
  nextHistoryPageQuery,
  normalizeHistoryLimit,
  parseTimeframe,
  parseVenue,
  HistoryQueryError,
  HISTORY_DEFAULT_LIMIT,
  HISTORY_MAX_LIMIT,
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

export {
  listTfPolicies,
  getTfPolicy,
  resolveVenueNativeInterval,
  assertAllVenueNative,
} from './policy/index.js';
export type { TfSourceMode, TfPolicyEntry } from './policy/index.js';
export {
  RestWeightBudget,
  KLINES_REQUEST_WEIGHT,
  DEFAULT_WEIGHT_LIMIT_PER_MIN,
  DEFAULT_GAP_FILL_RESERVE,
  getSharedRestWeightBudget,
  setSharedRestWeightBudget,
} from './binance/weight-budget.js';
export type { RestWeightBudgetOptions } from './binance/weight-budget.js';
export {
  computeWsReconnectDelayMs,
  computeRestBackoffMs,
  withRestRetry,
  WS_INITIAL_BACKOFF_MS,
  WS_MAX_BACKOFF_MS,
  REST_INITIAL_BACKOFF_MS,
  REST_MAX_BACKOFF_MS,
  REST_MAX_RETRIES,
  RATE_LIMIT_RECONNECT_POLICY_SUMMARY,
} from './binance/reconnect-policy.js';
export type {
  BackoffOptions,
  RestRetryPolicyOptions,
} from './binance/reconnect-policy.js';
export {
  backfillTimeframes,
  backfillAllTimeframes,
} from './backfill/index.js';
export type {
  FetchKlinesFn as BackfillFetchKlinesFn,
  BackfillTimeframeSpec,
  MultiTfBackfillOptions,
  TfBackfillResult,
  MultiTfBackfillResult,
} from './backfill/index.js';
export { startMultiTfLiveIngest } from './live/index.js';
export type {
  MultiTfLiveIngestOptions,
  MultiTfLiveIngestHandle,
} from './live/index.js';
