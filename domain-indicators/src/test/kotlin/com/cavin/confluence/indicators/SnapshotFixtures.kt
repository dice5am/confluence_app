package com.cavin.confluence.indicators

import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

internal object SnapshotFixtures {
    fun cutoff(): SnapshotCutoff = SnapshotCutoff.PACKAGED_2026_09_19

    fun loadMetaCutoff(): SnapshotCutoff {
        val meta = JSONObject(read("meta.json"))
        val packaged = SnapshotCutoff.PACKAGED_2026_09_19
        return SnapshotCutoff(
            cutoffMs = meta.getLong("cutoffMs"),
            cutoffUtcLabel = meta.getString("cutoffUtc"),
            cutoffTorontoLabel = packaged.cutoffTorontoLabel,
            snapshotVersion = packaged.snapshotVersion,
        )
    }

    fun loadBars(timeframeWire: String): List<IndicatorBar> {
        val root = JSONObject(read("BTCUSDT_$timeframeWire.json"))
        val arr = root.getJSONArray("candles")
        val out = ArrayList<IndicatorBar>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                IndicatorBar(
                    openTimeMs = o.getLong("openTimeMs"),
                    closeTimeMs = o.getLong("closeTimeMs"),
                    open = o.getDouble("open"),
                    high = o.getDouble("high"),
                    low = o.getDouble("low"),
                    close = o.getDouble("close"),
                    volume = o.getDouble("volume"),
                    isFinal = o.optBoolean("isFinal", true),
                ),
            )
        }
        return out
    }

    private fun read(name: String): String {
        val fromClasspath = SnapshotFixtures::class.java.classLoader.getResourceAsStream(name)
        if (fromClasspath != null) {
            return fromClasspath.bufferedReader().use { it.readText() }
        }
        val dir = snapshotDir()
        return Files.readString(dir.resolve(name))
    }

    private fun snapshotDir(): Path {
        val candidates = listOf(
            Path.of("data/src/main/assets/md_snapshot"),
            Path.of("../data/src/main/assets/md_snapshot"),
            Path.of("services/market-data/fixtures/snapshots"),
        )
        return candidates.firstOrNull { Files.isRegularFile(it.resolve("meta.json")) }
            ?: error("md_snapshot fixtures not found on classpath or disk")
    }
}
