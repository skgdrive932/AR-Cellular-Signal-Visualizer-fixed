package com.example.arsignal

import android.graphics.Color

object HeatmapColorMapper {

    /**
     * Maps dBm signal strength values to an AR color overlay.
     * Strong signal (-50 dBm or higher) -> Green
     * Moderate signal (-85 dBm) -> Yellow
     * Weak signal (-110 dBm or lower) -> Red
     */
    fun getColorForSignal(dbm: Int): Int {
        return when {
            dbm >= -60 -> Color.GREEN                  // Excellent Signal
            dbm in -75..-61 -> Color.CYAN              // Good Signal
            dbm in -90..-76 -> Color.YELLOW            // Fair Signal
            dbm in -105..-91 -> Color.rgb(255, 165, 0) // Poor Signal (Orange)
            else -> Color.RED                          // Very Poor / No Signal
        }
    }
}
