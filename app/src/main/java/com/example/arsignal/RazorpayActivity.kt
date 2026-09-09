package com.example.arsignal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arsignal.R
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class RazorpayActivity : AppCompatActivity(), PaymentResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_razorpay)

        Checkout.preload(applicationContext)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnPay = findViewById<Button>(R.id.btnStartPayment)

        btnPay.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            if (amountText.isNotEmpty()) {
                val amountInPaise = amountText.toDouble() * 100
                startPayment(amountInPaise)
            } else {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPayment(amountInPaise: Double) {
        val checkout = Checkout()
        
        checkout.setKeyID("rzp_test_YOUR_KEY_HERE")

        try {
            val options = JSONObject()
            options.put("name", "AR Cellular Signal Visualizer")
            options.put("description", "App Subscription Fee")
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")
            options.put("amount", amountInPaise)

            val prefill = JSONObject()
            prefill.put("email", "user@example.com")
            prefill.put("contact", "9876543210")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Toast.makeText(this, "Payment Successful! ID: $razorpayPaymentID", Toast.LENGTH_LONG).show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}
