package com.example.arsignal

import android.content.Context
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager

data class SignalInfo(
    val dbm: Int?,
    val networkType: String
)

class SignalReader(private val context: Context) {

    fun read(): SignalInfo {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var currentDbm: Int? = null
        var netType = "Unknown"

        try {
            val cellInfoList = telephonyManager.allCellInfo
            if (!cellInfoList.isNullOrEmpty()) {
                for (info in cellInfoList) {
                    if (info.isRegistered) {
                        when (info) {
                            is CellInfoLte -> {
                                currentDbm = info.cellSignalStrength.dbm
                                netType = "4G LTE"
                            }
                            is CellInfoNr -> {
                                currentDbm = info.cellSignalStrength.dbm
                                netType = "5G NR"
                            }
                            is CellInfoWcdma -> {
                                currentDbm = info.cellSignalStrength.dbm
                                netType = "3G WCDMA"
                            }
                            is CellInfoGsm -> {
                                currentDbm = info.cellSignalStrength.dbm
                                netType = "2G GSM"
                            }
                        }
                        break
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return SignalInfo(currentDbm, netType)
    }
}
