package com.example.arsignalvisualizer // Apne package name ke hisab se change karein

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val UPI_PAYMENT_REQUEST_CODE = 101

    // Yahan apni VPA/UPI ID aur Name dalein
    private val upiId = "yourname@upi" 
    private val name = "Kaushal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnPay = findViewById<Button>(R.id.btnPayUPI)

        btnPay.setOnClickListener {
            val amount = etAmount.text.toString().trim()
            if (amount.isNotEmpty()) {
                payWithUpi(amount)
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun payWithUpi(amount: String) {
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("mc", "")
            .appendQueryParameter("tr", System.currentTimeMillis().toString())
            .appendQueryParameter("tn", "App Payment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val upiPayIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }

        val chooser = Intent.createChooser(upiPayIntent, "Pay with")
        if (chooser.resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE)
        } else {
            Toast.makeText(this, "No UPI app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            if (data != null) {
                val trxt = data.getStringExtra("response")
                if (trxt != null && (trxt.contains("SUCCESS", ignoreCase = true) || trxt.contains("Status=SUCCESS", ignoreCase = true))) {
                    Toast.makeText(this, "Transaction Successful!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Transaction Failed or Cancelled", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
