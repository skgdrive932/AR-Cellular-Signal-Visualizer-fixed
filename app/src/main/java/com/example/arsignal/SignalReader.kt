package com.example.arsignal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class SignalInfo(
    val dbm: Int?,
    val networkType: String
)

class SignalReader(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phoneState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && phoneState
    }

    fun read(): SignalInfo {
        if (!hasRequiredPermissions()) {
            return SignalInfo(null, "No permissions granted")
        }

        var dbm: Int? = null
        var networkType = "Unknown"

        try {
            // Fetch Network Type Name
            networkType = getNetworkTypeName()

            // Fetch Cell Info for Signal Strength (dBm)
            val cellInfoList = telephonyManager.allCellInfo
            if (!cellInfoList.isNullOrEmpty()) {
                for (info in cellInfoList) {
                    if (info.isRegistered) {
                        when (info) {
                            is CellInfoLte -> dbm = info.cellSignalStrength.dbm
                            is CellInfoGsm -> dbm = info.cellSignalStrength.dbm
                            is CellInfoWcdma -> dbm = info.cellSignalStrength.dbm
                            is CellInfoNr -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    dbm = info.cellSignalStrength.dbm
                                }
                            }
                        }
                        if (dbm != null && dbm != Int.MAX_VALUE) break
                    }
                }
            }

            // Fallback for Signal Strength if allCellInfo returns null
            if (dbm == null || dbm == Int.MAX_VALUE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val signalStrength = telephonyManager.signalStrength
                    if (signalStrength != null) {
                        val cellSignalStrengths = signalStrength.cellSignalStrengths
                        if (cellSignalStrengths.isNotEmpty()) {
                            dbm = cellSignalStrengths[0].dbm
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            return SignalInfo(null, "Security Exception")
        } catch (e: Exception) {
            return SignalInfo(null, "Error reading signal")
        }

        if (dbm == Int.MAX_VALUE || dbm == null) {
            return SignalInfo(null, if (networkType.isNotEmpty()) networkType else "No registered cell")
        }

        return SignalInfo(dbm, networkType)
    }

    private fun getNetworkTypeName(): String {
        return try {
            val networkType = telephonyManager.dataNetworkType
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                else -> telephonyManager.networkOperatorName.ifEmpty { "Cellular Network" }
            }
        } catch (e: Exception) {
            "Cellular Network"
        }
    }
}
