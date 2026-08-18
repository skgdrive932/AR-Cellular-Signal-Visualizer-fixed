package com.example.arcellular

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class SignalInfo(
    val dbm: Int?,
    val networkType: String,
    val registered: Boolean
)

class SignalReader(private val context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun read(): SignalInfo {
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!hasLocation && !hasPhone) return SignalInfo(null, "Permission required", false)

        return try {
            val list: List<CellInfo>? = telephonyManager.allCellInfo
            val registered = list.orEmpty().firstOrNull { it.isRegistered }
            if (registered == null) return SignalInfo(null, "No registered cell", false)
            when (registered) {
                is CellInfoNr -> SignalInfo(registered.cellSignalStrength.dbm, "5G NR", true)
                is CellInfoLte -> SignalInfo(registered.cellSignalStrength.dbm, "4G LTE", true)
                is CellInfoWcdma -> SignalInfo(registered.cellSignalStrength.dbm, "3G WCDMA", true)
                is CellInfoGsm -> SignalInfo(registered.cellSignalStrength.dbm, "2G GSM", true)
                else -> SignalInfo(registered.cellSignalStrength.dbm, "Cellular", true)
            }
        } catch (_: SecurityException) {
            SignalInfo(null, "Permission required", false)
        } catch (_: Exception) {
            SignalInfo(null, "Unavailable", false)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val location = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val phone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        } else true
        return location && phone
    }
}
