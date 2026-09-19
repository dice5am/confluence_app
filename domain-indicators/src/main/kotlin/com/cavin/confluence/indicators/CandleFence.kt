package com.cavin.confluence.indicators

/**
 * Required APPLY fence: drop forming bars and any bar with
 * `closeTimeMs > cutoffMs` (1m/5m snapshot files currently overshoot).
 *
 * Kept bars: `isFinal == true && closeTimeMs <= cutoffMs`, ordered by `openTimeMs`.
 */
object CandleFence {

    fun apply(bars: List<IndicatorBar>, cutoff: SnapshotCutoff): FencedWindow {
        var forming = 0
        var overshoot = 0
        val kept = ArrayList<IndicatorBar>(bars.size)
        for (bar in bars) {
            if (!bar.isFinal) {
                forming += 1
                continue
            }
            if (bar.closeTimeMs > cutoff.cutoffMs) {
                overshoot += 1
                continue
            }
            kept.add(bar)
        }
        kept.sortBy { it.openTimeMs }
        return FencedWindow(
            cutoff = cutoff,
            inputCount = bars.size,
            bars = kept,
            droppedFormingCount = forming,
            droppedOvershootCount = overshoot,
        )
    }
}

data class FencedWindow(
    val cutoff: SnapshotCutoff,
    val inputCount: Int,
    val bars: List<IndicatorBar>,
    val droppedFormingCount: Int,
    val droppedOvershootCount: Int,
) {
    val keptCount: Int get() = bars.size

    fun report(): FenceReport = FenceReport(
        cutoffMs = cutoff.cutoffMs,
        cutoffUtcLabel = cutoff.cutoffUtcLabel,
        cutoffTorontoLabel = cutoff.cutoffTorontoLabel,
        snapshotVersion = cutoff.snapshotVersion,
        inputCount = inputCount,
        keptCount = keptCount,
        droppedFormingCount = droppedFormingCount,
        droppedOvershootCount = droppedOvershootCount,
        firstOpenTimeMs = bars.firstOrNull()?.openTimeMs,
        lastCloseTimeMs = bars.lastOrNull()?.closeTimeMs,
    )
}

data class FenceReport(
    val cutoffMs: Long,
    val cutoffUtcLabel: String,
    val cutoffTorontoLabel: String,
    val snapshotVersion: String,
    val inputCount: Int,
    val keptCount: Int,
    val droppedFormingCount: Int,
    val droppedOvershootCount: Int,
    val firstOpenTimeMs: Long?,
    val lastCloseTimeMs: Long?,
)
