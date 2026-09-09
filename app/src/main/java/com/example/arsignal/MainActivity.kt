package com.example.arsignalvisualizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val UPI_PAYMENT_REQUEST_CODE = 101

    // Apni UPI ID aur Naam yahan set karein
    private val upiId = "yourname@upi"
    private val payeeName = "Kaushal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnUpi = findViewById<Button>(R.id.btnUpiIntent)
        val btnRazorpay = findViewById<Button>(R.id.btnRazorpay)

        // 1. Direct UPI App (GPay/PhonePe) kholne ke liye
        btnUpi.setOnClickListener {
            payWithUpiIntent("100.00") // Set default amount
        }

        // 2. Razorpay Gateway Screen par jaane ke liye
        btnRazorpay.setOnClickListener {
            val intent = Intent(this, RazorpayActivity::class.java)
            startActivity(intent)
        }
    }

    private fun payWithUpiIntent(amount: String) {
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("mc", "")
            .appendQueryParameter("tr", System.currentTimeMillis().toString())
            .appendQueryParameter("tn", "App Payment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val upiPayIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }

        val chooser = Intent.createChooser(upiPayIntent, "Pay with UPI")
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
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
