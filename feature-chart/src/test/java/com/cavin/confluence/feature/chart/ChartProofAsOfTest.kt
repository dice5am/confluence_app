package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.snapshot.MdSnapshotStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChartProofAsOfTest {
    @Test
    fun proofAsOfUsesPackagedMetaCutoffNotPr22() {
        assertEquals(MdSnapshotStore.bannerLabel, ChartProofAsOf)
        assertEquals(
            "Historical snapshot · md_snapshot@2026-09-19 · 2026-09-19 10:59 UTC",
            ChartProofAsOf,
        )
        assertEquals(1_789_815_599_999L, MdSnapshotStore.PACKAGED_CUTOFF_MS)
        assertFalse(ChartProofAsOf.contains("LIVE", ignoreCase = true))
        assertFalse(ChartHonesty.packagedAsOfCaption.contains("LIVE", ignoreCase = true))
        assertEquals("SNAPSHOT", ChartHonesty.SNAPSHOT_BADGE)
        assertEquals("Snapshot · 2026-09-19 10:59 UTC", ChartHonesty.packagedAsOfCaption)
        assertEquals("Overlay-first · RSI only", ChartHonesty.MODE_CAPTION)
        assertEquals("md_snapshot@2026-09-19", MdSnapshotStore.PACKAGED_SNAPSHOT_VERSION)
        assertFalse(ChartHonesty.SNAPSHOT_BADGE.equals("LIVE", ignoreCase = true))
    }
}
