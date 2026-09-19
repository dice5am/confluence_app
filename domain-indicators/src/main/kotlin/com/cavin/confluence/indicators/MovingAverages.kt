package com.cavin.confluence.indicators

/** Simple moving average. First defined value is at index `period - 1`. */
object Sma {
    fun series(values: DoubleArray, period: Int): List<Double?> {
        require(period > 0)
        val out = MutableList<Double?>(values.size) { null }
        if (values.size < period) return out
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]
            if (i >= period) {
                sum -= values[i - period]
            }
            if (i >= period - 1) {
                out[i] = sum / period
            }
        }
        return out
    }

    data class Accumulator(
        val period: Int,
        val window: DoubleArray,
        val filled: Int,
        val nextIndex: Int,
        val sum: Double,
    ) {
        companion object {
            fun empty(period: Int): Accumulator {
                require(period > 0)
                return Accumulator(period, DoubleArray(period), 0, 0, 0.0)
            }
        }
    }

    fun next(state: Accumulator, value: Double): Pair<Accumulator, Double?> {
        val window = state.window.copyOf()
        return if (state.filled < state.period) {
            window[state.filled] = value
            val filled = state.filled + 1
            val sum = state.sum + value
            val defined = if (filled == state.period) sum / state.period else null
            Accumulator(state.period, window, filled, 0, sum) to defined
        } else {
            val idx = state.nextIndex
            val sum = state.sum - window[idx] + value
            window[idx] = value
            val nextIndex = (idx + 1) % state.period
            Accumulator(state.period, window, state.filled, nextIndex, sum) to (sum / state.period)
        }
    }
}

/**
 * Standard EMA: seed = SMA of the first `period` values, then
 * `ema = value * k + prev * (1 - k)` with `k = 2 / (period + 1)`.
 */
object Ema {
    fun multiplier(period: Int): Double {
        require(period > 0)
        return 2.0 / (period + 1)
    }

    fun series(values: DoubleArray, period: Int): List<Double?> {
        require(period > 0)
        val out = MutableList<Double?>(values.size) { null }
        if (values.size < period) return out
        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        var ema = sum / period
        out[period - 1] = ema
        val k = multiplier(period)
        val oneMinus = 1.0 - k
        for (i in period until values.size) {
            ema = values[i] * k + ema * oneMinus
            out[i] = ema
        }
        return out
    }

    data class Accumulator(
        val period: Int,
        val k: Double,
        val seedSum: Double,
        val seedCount: Int,
        val ema: Double,
        val seeded: Boolean,
    ) {
        companion object {
            fun empty(period: Int): Accumulator {
                require(period > 0)
                return Accumulator(period, Ema.multiplier(period), 0.0, 0, 0.0, false)
            }
        }
    }

    fun next(state: Accumulator, value: Double): Pair<Accumulator, Double?> {
        return if (!state.seeded) {
            val seedCount = state.seedCount + 1
            val seedSum = state.seedSum + value
            if (seedCount < state.period) {
                state.copy(seedSum = seedSum, seedCount = seedCount) to null
            } else {
                val ema = seedSum / state.period
                state.copy(seedSum = 0.0, seedCount = seedCount, ema = ema, seeded = true) to ema
            }
        } else {
            val ema = value * state.k + state.ema * (1.0 - state.k)
            state.copy(ema = ema) to ema
        }
    }
}

/** Day-one MA set: EMA 9/21, SMA 50, SMA 200 on close (ALT-1.2 §3.3). */
object MovingAverages {
    const val EMA_FAST: Int = 9
    const val EMA_SLOW: Int = 21
    const val SMA_MID: Int = 50
    const val SMA_LONG: Int = 200
}
