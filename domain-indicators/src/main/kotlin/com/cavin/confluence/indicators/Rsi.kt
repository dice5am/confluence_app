package com.cavin.confluence.indicators

/**
 * RSI using Wilder smoothing on close (ALT-1.2 §3.1).
 *
 * First value needs `period` changes (i.e. `period + 1` closes). Seed averages
 * are the SMA of those first `period` gains/losses; subsequent bars use
 * Wilder: `(prev * (period - 1) + current) / period`.
 *
 * If `avgLoss == 0` → RSI = 100. Output is in `[0, 100]` when defined.
 */
object Rsi {
    const val DEFAULT_PERIOD: Int = 14

    data class Accumulator(
        val period: Int,
        val lastClose: Double,
        val avgGain: Double,
        val avgLoss: Double,
        val seeded: Boolean,
        val seedCount: Int,
        val seedGainSum: Double,
        val seedLossSum: Double,
    )

    fun series(closes: DoubleArray, period: Int = DEFAULT_PERIOD): List<Double?> {
        require(period > 0)
        val out = MutableList<Double?>(closes.size) { null }
        var acc: Accumulator? = null
        for (i in closes.indices) {
            val step = next(acc, closes[i], period)
            acc = step.first
            out[i] = step.second
        }
        return out
    }

    fun next(
        state: Accumulator?,
        close: Double,
        period: Int = DEFAULT_PERIOD,
    ): Pair<Accumulator, Double?> {
        require(period > 0)
        if (state == null) {
            return Accumulator(
                period = period,
                lastClose = close,
                avgGain = 0.0,
                avgLoss = 0.0,
                seeded = false,
                seedCount = 0,
                seedGainSum = 0.0,
                seedLossSum = 0.0,
            ) to null
        }
        val change = close - state.lastClose
        val gain = if (change > 0.0) change else 0.0
        val loss = if (change < 0.0) -change else 0.0
        return if (!state.seeded) {
            val gainSum = state.seedGainSum + gain
            val lossSum = state.seedLossSum + loss
            val n = state.seedCount + 1
            if (n < period) {
                Accumulator(
                    period = period,
                    lastClose = close,
                    avgGain = 0.0,
                    avgLoss = 0.0,
                    seeded = false,
                    seedCount = n,
                    seedGainSum = gainSum,
                    seedLossSum = lossSum,
                ) to null
            } else {
                val avgGain = gainSum / period
                val avgLoss = lossSum / period
                Accumulator(
                    period = period,
                    lastClose = close,
                    avgGain = avgGain,
                    avgLoss = avgLoss,
                    seeded = true,
                    seedCount = n,
                    seedGainSum = 0.0,
                    seedLossSum = 0.0,
                ) to value(avgGain, avgLoss)
            }
        } else {
            val avgGain = (state.avgGain * (period - 1) + gain) / period
            val avgLoss = (state.avgLoss * (period - 1) + loss) / period
            Accumulator(
                period = period,
                lastClose = close,
                avgGain = avgGain,
                avgLoss = avgLoss,
                seeded = true,
                seedCount = state.seedCount + 1,
                seedGainSum = 0.0,
                seedLossSum = 0.0,
            ) to value(avgGain, avgLoss)
        }
    }

    internal fun value(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
