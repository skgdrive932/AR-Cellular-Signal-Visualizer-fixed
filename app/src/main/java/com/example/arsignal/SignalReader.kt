package com.example.arcellular

import android.content.Context
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import android.telephony.TelephonyManager

data class SignalInfo(
    val dbm: Int?,
    val networkType: String
)

class SignalReader(private val context: Context) {

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun read(): SignalInfo {
        var dbm: Int? = null
        var networkType = getNetworkTypeName()

        try {
            val cellInfoList = telephonyManager.allCellInfo
            if (!cellInfoList.isNullOrEmpty()) {
                for (info in cellInfoList) {
                    if (info.isRegistered) {
                        when (info) {
                            is CellInfoLte -> {
                                dbm = info.cellSignalStrength.dbm
                                networkType = "4G LTE"
                                break
                            }
                            is CellInfoNr -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val nrStrength = info.cellSignalStrength as? CellSignalStrengthNr
                                    dbm = nrStrength?.dbm
                                }
                                networkType = "5G NR"
                                break
                            }
                            is CellInfoWcdma -> {
                                dbm = info.cellSignalStrength.dbm
                                networkType = "3G (WCDMA)"
                                break
                            }
                            is CellInfoGsm -> {
                                dbm = info.cellSignalStrength.dbm
                                networkType = "2G (GSM)"
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return SignalInfo(dbm = dbm, networkType = networkType)
    }

    private fun getNetworkTypeName(): String {
        return try {
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                else -> "Unknown/Checking"
            }
        } catch (e: SecurityException) {
            "Permission Required"
        }
    }
}
