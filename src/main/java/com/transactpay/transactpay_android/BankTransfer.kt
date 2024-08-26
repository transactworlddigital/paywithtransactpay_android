package com.transactpay.transactpay_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class BankTransfer : AppCompatActivity() {

    private lateinit var countdownTextView: TextView
    private var orderStatus: Boolean = true

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bank_transfer)

        val merchantName = intent.getStringExtra("MERCHANT_NAME")
        val amountString = intent.getStringExtra("AMOUNT")
        val email = intent.getStringExtra("EMAIL")
        val bankName = intent.getStringExtra("BankName")
        val accountNumber = intent.getStringExtra("RecipientAccount")
        val apiKey = intent.getStringExtra("API_KEY")
        val baseurl = intent.getStringExtra("BASEURL")
        val inititingClass = intent.getStringExtra("INITIATING_ACTIVITY_CLASS") as Class<*>
        val success = intent.getStringExtra("SUCCESS") as Class<*>
        val failed = intent.getStringExtra("FAILED") as Class<*>
        val encryptKey: String = intent.getStringExtra("XMLKEY").toString()
        val referenceNumber: String = intent.getStringExtra("REFERENCE_NUMBER").toString()

        Log.d(TAG, "Encryption key is : $encryptKey")

        val rsaPublicKeyXml = EncryptionUtils.decodeBase64AndExtractKey(encryptKey)

        Log.d(TAG, "Amount here is : $amountString")
        val amount = amountString?.toDoubleOrNull()
        val formattedAmount = formatAmount(amount)

        // Use the data as needed in this activity
        findViewById<TextView>(R.id.merchantNameTextView).text = merchantName
        findViewById<TextView>(R.id.shopName).text = merchantName
        findViewById<TextView>(R.id.amountTextView).text = formattedAmount
        findViewById<TextView>(R.id.emailTextView).text = email
        findViewById<TextView>(R.id.accountNumber).text = accountNumber
        findViewById<TextView>(R.id.bankName).text = bankName
        countdownTextView = findViewById(R.id.countdown)

        val urlBank = "$baseurl/order/status"

        // Countdown Timer Logic
        val countdownTime = 10 * 60 * 1000L
        val timer = object : CountDownTimer(countdownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                countdownTextView.text = "Account details are valid for this transaction only and will expire in $timeFormatted minutes"
            }

            override fun onFinish() {
                countdownTextView.text = "The transaction has expired."
                findViewById<Button>(R.id.payButton).isEnabled = false
            }
        }
        timer.start()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (orderStatus) {
                    delay(10_000L) // Poll every 10 seconds
                    try {
                        val response = pollCheckTransaction(urlBank, apiKey.toString(), rsaPublicKeyXml, referenceNumber)
                        val jObject = JSONObject(response)

                        if (jObject.getString("status") == "Successful") {
                            orderStatus = false

                            withContext(Dispatchers.Main) {
                                // Convert JSONObject to String
                                val jsonString = jObject.toString()

                                // Create an Intent to start the Success Activity
                                val intent = Intent(this@BankTransfer, success).apply {
                                    putExtra("json_data", jsonString) // Attach JSON String as an extra
                                }
                                startActivity(intent)
                                finish() // Optional: finish the current activity if you don't need it anymore
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in Coroutine: ${e.message}")
                        // Optionally handle or log specific errors here
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Coroutine: ${e.message}")
            }
        }

        findViewById<Button>(R.id.payButton).setOnClickListener {
            findViewById<LinearLayout>(R.id.accountDetails).visibility = View.GONE
            findViewById<ImageView>(R.id.backToOptions).visibility = View.GONE
            findViewById<Button>(R.id.payButton).visibility = View.GONE
            findViewById<Button>(R.id.viewAccount).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.processing).visibility = View.VISIBLE
            findViewById<TextView>(R.id.completeText).visibility = View.GONE
        }

        findViewById<Button>(R.id.viewAccount).setOnClickListener {
            findViewById<LinearLayout>(R.id.accountDetails).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.backToOptions).visibility = View.VISIBLE
            findViewById<Button>(R.id.payButton).visibility = View.VISIBLE
            findViewById<TextView>(R.id.completeText).visibility = View.VISIBLE
            findViewById<Button>(R.id.viewAccount).visibility = View.GONE
            findViewById<LinearLayout>(R.id.processing).visibility = View.GONE
        }
    }

    private suspend fun pollCheckTransaction(
        statusUrl: String,
        apiKey: String,
        publicKeyXml: String,
        reference: String
    ): String? {
        return try {
            val orderPayload = """
                {
                    "reference": "$reference"
                }
            """.trimIndent()

            val orderEncrypted = EncryptionUtils.encryptPayloadRSA(orderPayload, publicKeyXml) ?: throw Exception("Encryption failed")

            val orderJson = """
                {
                    "data": "$orderEncrypted"
                }
            """.trimIndent()

            Log.d(TAG, "Order payload : ${orderPayload}")
            Log.d(TAG, "Public key is payload : ${orderEncrypted}")
            Log.d(TAG, "API Order key is payload : ${apiKey}")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val orderRequestBody: RequestBody = orderJson.toRequestBody(mediaType)

            val client = OkHttpClient()
            val requestOrder = Request.Builder()
                .url(statusUrl)
                .post(orderRequestBody)
                .addHeader("accept", "application/json")
                .addHeader("api-key", "$apiKey")
                .addHeader("content-type", "application/json")
                .build()

            val response = client.newCall(requestOrder).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Response Body: $responseBody")

            if (response.isSuccessful) {
                responseBody
            } else {
                Log.e(TAG, "Response unsuccessful: ${response.message}")
                responseBody
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during pollCheckTransaction: ${e.message}")
            "Exception: ${e.message}"
        }
    }

    private fun formatAmount(amount: Double?): String {
        val numberFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault())
        numberFormat.maximumFractionDigits = 2
        numberFormat.minimumFractionDigits = 2
        return amount?.let {
            val amountFormatted = numberFormat.format(it)
            "NGN $amountFormatted"
        } ?: "Invalid amount"
    }

    companion object {
        const val TAG = "BankTransfer"
    }
}
