package com.example.arsignal

import android.net.TrafficStats
import android.os.SystemClock

data class SpeedInfo(
    val speedKbps: Double,
    val speedMbps: Double,
    val formattedSpeed: String
)

class NetworkSpeedMonitor {

    private var lastTotalRxBytes: Long = 0L
    private var lastTimeStamp: Long = 0L

    init {
        start()
    }

    fun start() {
        lastTotalRxBytes = TrafficStats.getTotalRxBytes()
        lastTimeStamp = SystemClock.elapsedRealtime()
    }

    fun getDownloadSpeed(): SpeedInfo {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTime = SystemClock.elapsedRealtime()

        if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            return SpeedInfo(0.0, 0.0, "Speed: N/A")
        }

        val bytesTransferred = currentRxBytes - lastTotalRxBytes
        val timeDiffInSeconds = (currentTime - lastTimeStamp) / 1000.0

        // Reset baseline for next calculation
        lastTotalRxBytes = currentRxBytes
        lastTimeStamp = currentTime

        if (timeDiffInSeconds <= 0 || bytesTransferred < 0) {
            return SpeedInfo(0.0, 0.0, "0 Kbps")
        }

        // Calculation: Bytes/sec to Bits/sec (Multiply by 8)
        val bitsPerSecond = (bytesTransferred * 8) / timeDiffInSeconds
        val kbps = bitsPerSecond / 1000.0
        val mbps = kbps / 1000.0

        val formattedText = if (mbps >= 1.0) {
            String.format("%.2f Mbps", mbps)
        } else {
            String.format("%.1f Kbps", kbps)
        }

        return SpeedInfo(kbps, mbps, formattedText)
    }
}
