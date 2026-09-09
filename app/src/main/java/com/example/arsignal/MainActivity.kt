package com.example.arsignal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arsignal.R

class MainActivity : AppCompatActivity() {

    private val UPI_PAYMENT_REQUEST_CODE = 101

    private val upiId = "santosh.kaushal@ptaxis"
    private val payeeName = "SK Kaushal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn2D = findViewById<Button>(R.id.btn2DFeature) // Apna 2D Button ID rakhein
        val btn3D = findViewById<Button>(R.id.btn3DFeature) // Apna 3D Button ID rakhein

        // 1. 2D Feature - Always Free
        btn2D?.setOnClickListener {
            open2DVisualizer()
        }

        // 2. 3D Premium Feature - Check Payment Status
        btn3D?.setOnClickListener {
            if (isPremiumUser()) {
                open3DVisualizer()
            } else {
                Toast.makeText(this, "3D Feature is Premium! Please pay to unlock.", Toast.LENGTH_LONG).show()
                payWithUpiIntent("100.00") // Trigger Payment
            }
        }
    }

    private fun open2DVisualizer() {
        Toast.makeText(this, "Opening 2D Signal Mode...", Toast.LENGTH_SHORT).show()
        // Yahan apni 2D Activity launch karein
    }

    private fun open3DVisualizer() {
        Toast.makeText(this, "Opening 3D AR Signal Visualizer!", Toast.LENGTH_SHORT).show()
        // Yahan apni 3D / AR Activity launch karein
    }

    private fun payWithUpiIntent(amount: String) {
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("mc", "")
            .appendQueryParameter("tr", System.currentTimeMillis().toString())
            .appendQueryParameter("tn", "Unlock 3D Premium Feature")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val upiPayIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }

        val chooser = Intent.createChooser(upiPayIntent, "Pay with UPI to Unlock 3D Mode")
        if (chooser.resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE)
        } else {
            Toast.makeText(this, "No UPI app found on your phone", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            if (data != null) {
                val response = data.getStringExtra("response")
                if (response != null && (response.contains("SUCCESS", ignoreCase = true) || response.contains("Status=SUCCESS", ignoreCase = true))) {
                    
                    // Payment successful -> Save Premium Status permanently
                    setPremiumUserStatus(true)
                    Toast.makeText(this, "Payment Successful! 3D Feature Unlocked 🎉", Toast.LENGTH_LONG).show()
                    
                    // Direct 3D Feature open karein
                    open3DVisualizer()

                } else {
                    Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper functions for Local Storage
    private fun isPremiumUser(): Boolean {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_3D_Unlocked", false)
    }

    private fun setPremiumUserStatus(isUnlocked: Boolean) {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_3D_Unlocked", isUnlocked)
            apply()
        }
    }
}
