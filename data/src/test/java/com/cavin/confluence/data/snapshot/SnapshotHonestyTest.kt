package com.cavin.confluence.data.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotHonestyTest {
    @Test
    fun packagedCutoffIsPr24Tip() {
        assertEquals("2026-09-11 14:59 UTC", MdSnapshotStore.PACKAGED_CUTOFF_UTC)
        assertTrue(MdSnapshotStore.bannerLabel.contains("2026-09-11 14:59 UTC"))
        assertEquals(
            "Historical snapshot · as of 2026-09-11 14:59 UTC",
            MdSnapshotStore.bannerLabel,
        )
    }
}
