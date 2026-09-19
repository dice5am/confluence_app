package com.cavin.confluence.data.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotHonestyTest {
    @Test
    fun packagedCutoffIsPr29Tip() {
        assertEquals("md_snapshot@2026-09-19", MdSnapshotStore.PACKAGED_SNAPSHOT_VERSION)
        assertEquals("2026-09-19 10:59 UTC", MdSnapshotStore.PACKAGED_CUTOFF_UTC)
        assertEquals(1_789_815_599_999L, MdSnapshotStore.PACKAGED_CUTOFF_MS)
        assertTrue(MdSnapshotStore.bannerLabel.contains("2026-09-19 10:59 UTC"))
        assertTrue(MdSnapshotStore.bannerLabel.contains("md_snapshot@2026-09-19"))
        assertEquals(
            "Historical snapshot · md_snapshot@2026-09-19 · 2026-09-19 10:59 UTC",
            MdSnapshotStore.bannerLabel,
        )
    }
}
