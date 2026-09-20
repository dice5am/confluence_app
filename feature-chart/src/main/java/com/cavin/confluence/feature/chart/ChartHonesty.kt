package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.SnapshotCutoff
import java.util.Locale

/**
 * Phase B V2 snapshot honesty copy. Frozen SNAPSHOT chrome — never LIVE.
 * As-of labels pass through packaged `md_snapshot@2026-09-19` cutoff.
 */
object ChartHonesty {
    const val SNAPSHOT_BADGE: String = "SNAPSHOT"
    const val MODE_CAPTION: String = "Overlay-first · RSI only"
    const val FORBIDDEN_LIVE: String = "LIVE"
    const val PAIR: String = "BTCUSDT"

    fun asOfCaption(utcLabel: String): String = "Snapshot · $utcLabel"

    val packagedUtcLabel: String
        get() = MdSnapshotStore.cutoffUtcLabel.ifBlank { MdSnapshotStore.PACKAGED_CUTOFF_UTC }

    val packagedAsOfCaption: String
        get() = asOfCaption(packagedUtcLabel)

    fun formatLastPrice(close: Double): String = if (close >= 1_000.0) {
        String.format(Locale.US, "%,.0f", close)
    } else {
        String.format(Locale.US, "%.2f", close)
    }

    fun cutoffMatchesPackaged(cutoff: SnapshotCutoff): Boolean =
        cutoff.cutoffMs == SnapshotCutoff.PACKAGED_2026_09_19.cutoffMs &&
            cutoff.snapshotVersion == SnapshotCutoff.PACKAGED_2026_09_19.snapshotVersion
}
