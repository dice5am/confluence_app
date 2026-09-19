package com.cavin.confluence.indicators

/**
 * Frozen snapshot cutoff labels passed through from `meta.json` /
 * [docs/market-data/SNAPSHOT-CUTOFF.md]. The engine never invents labels.
 */
data class SnapshotCutoff(
    val cutoffMs: Long,
    val cutoffUtcLabel: String,
    val cutoffTorontoLabel: String,
    val snapshotVersion: String,
) {
    companion object {
        /** Packaged `md_snapshot@2026-09-19` (PR #29). */
        val PACKAGED_2026_09_19: SnapshotCutoff = SnapshotCutoff(
            cutoffMs = 1_789_815_599_999L,
            cutoffUtcLabel = "2026-09-19 10:59 UTC",
            cutoffTorontoLabel = "2026-09-19 06:59 EDT (America/Toronto, UTC-4)",
            snapshotVersion = "md_snapshot@2026-09-19",
        )
    }
}
