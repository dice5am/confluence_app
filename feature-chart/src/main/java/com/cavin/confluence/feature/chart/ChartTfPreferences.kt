package com.cavin.confluence.feature.chart

import android.content.Context
import com.cavin.confluence.data.model.Timeframe

/**
 * MOB-2.3 — persist last-used chart timeframe.
 * First-open default: [Timeframe.H1] (locked).
 */
class ChartTfPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLastUsedOrDefault(): Timeframe {
        val wire = prefs.getString(KEY_TF, null) ?: return DEFAULT
        return runCatching { Timeframe.fromWire(wire) }.getOrDefault(DEFAULT)
    }

    fun setLastUsed(tf: Timeframe) {
        prefs.edit().putString(KEY_TF, tf.wire).apply()
    }

    companion object {
        private const val PREFS = "confluence_chart"
        private const val KEY_TF = "last_used_tf"
        val DEFAULT: Timeframe = Timeframe.H1
    }
}
