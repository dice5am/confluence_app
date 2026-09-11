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
            "Historical snapshot · as of 2026-09-11 14:59 UTC",
            ChartProofAsOf,
        )
        assertFalse(ChartProofAsOf.contains("2026-09-10"))
        assertFalse(ChartProofAsOf.contains("19:59"))
    }
}
