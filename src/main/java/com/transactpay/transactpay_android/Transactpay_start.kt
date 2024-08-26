package com.transactpay.transactpay_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withContext

import org.json.JSONObject

class Transactpay_start : AppCompatActivity() {

    companion object {
        const val TAG = "Transactpay_start"
    }

    private var paymentOption: String? = null

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transactpay_start)

        val fname = intent.getStringExtra("Fname")
        val lname = intent.getStringExtra("Lname")
        val phone = intent.getStringExtra("Phone")
        val merchantName = intent.getStringExtra("MERCHANT_NAME")
        val amountString = intent.getStringExtra("AMOUNT")
        val email = intent.getStringExtra("EMAIL")
        val apiKey = intent.getStringExtra("APIKEY")
        val baseurl = intent.getStringExtra("BASEURL")
        val inititingClass = intent.getStringExtra("INITIATING_ACTIVITY_CLASS") as Class<*>
        val success = intent.getStringExtra("SUCCESS") as Class<*>
        val failed = intent.getStringExtra("FAILED") as Class<*>
        val rsaPublicKeyXml : String = intent.getStringExtra("XMLKEY").toString()
        val referenceNumber : String = intent.getStringExtra("REFERENCE_NUMBER").toString()

        Log.d(TAG, "Encryption key is NOW NOW is : $rsaPublicKeyXml")

        val newRSA = EncryptionUtils.decodeBase64AndExtractKey(rsaPublicKeyXml)

        val amount = amountString?.toDoubleOrNull()
        val formattedAmount = formatAmount(amount)

        findViewById<TextView>(R.id.merchantNameTextView).text = merchantName
        findViewById<TextView>(R.id.amountTextView).text = formattedAmount
        findViewById<TextView>(R.id.emailTextView).text = email
        val cardLayout: View? = findViewById(R.id.openCard)
        val transferLayout : View? = findViewById(R.id.Banktrasfer)
        var fee : Double = 0.0

        //handle card click
        cardLayout?.setOnClickListener {
            paymentOption = "Card"
            val url = "$baseurl/order/fee"
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = postEncryptedPayload(url, amount, "NGN", "C", newRSA, apiKey.toString())

                    Log.d(TAG, "Response is: $response")

                    // Attempt to parse the response string as JSON
                    val jsonResponse = JSONObject(response)

                    // Extract status and data
                    val status = jsonResponse.getString("status")
                    val data = jsonResponse.getJSONObject("data")

                    Log.d(TAG, "Final status is : $status")

                    // Switch to the main thread to update the UI
                    withContext(Dispatchers.Main) {
                        if (status == "success") {

                            fee = data.optString("fee")?.toDoubleOrNull()!!
                            val formattedAmount = formatAmount(fee)
                            // Hide the necessary views
                            findViewById<TextView>(R.id.Banktrasfer).visibility = View.GONE
                            findViewById<LinearLayout>(R.id.showFees).visibility = View.VISIBLE

                            // Show the fee
                            findViewById<TextView>(R.id.thisFee).setText(formattedAmount)

                        } else {
                            //redirect to failed page
                            val intent = Intent(this@Transactpay_start, failed).apply {
                                putExtra("status", jsonResponse.getString("status"))
                                putExtra("code", jsonResponse.getString("statusCode"))
                                putExtra("message", jsonResponse.getString("message"))
                            }
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "This is the error $e")
            }
        }

        //handle transfer click
        transferLayout?.setOnClickListener{
            paymentOption = "Bank-transfer"
            val url = "$baseurl/order/fee"
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = postEncryptedPayload(url, amount, "NGN", "bank-transfer", newRSA, apiKey.toString())

                    Log.d(TAG, "Response is: $response")

                    // Attempt to parse the response string as JSON
                    val jsonResponse = JSONObject(response)

                    // Extract status and data
                    val status = jsonResponse.getString("status")
                    val data = jsonResponse.getJSONObject("data")

                    Log.d(TAG, "Final status is : $status")

                    // Switch to the main thread to update the UI
                    withContext(Dispatchers.Main) {
                        if (status == "success") {

                            fee = data.optString("fee")?.toDoubleOrNull()!!
                            val formattedAmount = formatAmount(fee)
                            // Hide the necessary views
                            findViewById<LinearLayout>(R.id.openCard).visibility = View.GONE
                            findViewById<LinearLayout>(R.id.showFees).visibility = View.VISIBLE

                            // Show the fee
                            findViewById<TextView>(R.id.thisFee).setText(formattedAmount)

                        } else {
                            //redirect to failed page
                            val intent = Intent(this@Transactpay_start, failed).apply {
                                putExtra("status", jsonResponse.getString("status"))
                                putExtra("code", jsonResponse.getString("statusCode"))
                                putExtra("message", jsonResponse.getString("message"))
                            }
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "This is the error $e")
            }
        }

        //handle make payment
        findViewById<Button>(R.id.makePayment).setOnClickListener{

            val total = amountString?.toDoubleOrNull()?.plus(fee)
            Log.d(TAG, "Payment Amount is $amountString")
            Log.d(TAG, "Payment Fee is $fee")
            Log.d(TAG, "Payment Total is $total")

            // Determine the intent based on the selected payment option
            val intent = when(paymentOption) {
                "Card" -> Intent(this@Transactpay_start, CardActivity::class.java)
                "Bank-transfer" -> Intent(this@Transactpay_start, SelectBank::class.java) // Replace with your actual Bank Transfer Activity
                else -> null
            }

            intent?.apply {
                putExtra("Fname", fname)
                putExtra("Lname", lname)
                putExtra("Phone", phone)
                putExtra("MERCHANT_NAME", merchantName)
                putExtra("AMOUNT", total.toString())
                putExtra("EMAIL", email)
                putExtra("API_KEY", apiKey)
                putExtra("XMLKEY", rsaPublicKeyXml)
                putExtra("BASEURL", baseurl)
                putExtra("REFERENCE_NUMBER", referenceNumber)
                putExtra("INITIATING_ACTIVITY_CLASS", inititingClass)
                putExtra("SUCCESS", success)
                putExtra("FAILED", failed)
            }

            intent?.let {
                startActivity(it)
            } ?: run {
                Log.e(TAG, "No payment option selected")
            }
        }

        //handle Cancel payment
        findViewById<TextView>(R.id.cancelPayment).setOnClickListener{
            var intent = Intent(this@Transactpay_start, inititingClass)
            startActivity(intent)
        }

        //handle Change Payment Option
        findViewById<TextView>(R.id.changePaymentOption).setOnClickListener{
            if (paymentOption == "Card"){
                findViewById<TextView>(R.id.Banktrasfer).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.showFees).visibility = View.GONE
            }else if(paymentOption == "Bank-transfer"){
                findViewById<LinearLayout>(R.id.openCard).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.showFees).visibility = View.GONE
            }
        }
    }

    private suspend fun postEncryptedPayload(
        url: String,
        amount: Double?,
        currency: String,
        paymentOption: String,
        newRSA: String,
        apiKey : String
    ): String? {
        return try {
            val payload = """
                {
                    "amount": $amount,
                    "currency": "$currency",
                    "paymentoption": "$paymentOption"
                }
            """.trimIndent()

            Log.d(TAG,payload)

            val encryptedData = EncryptionUtils.encryptPayloadRSA(payload, newRSA) ?: throw Exception("Encryption failed")

            val json = """
                {
                    "data": "$encryptedData"
                }
            """.trimIndent()

            Log.d(TAG, json)

            Log.d(TAG, "API KEY IS: $apiKey")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody: RequestBody = json.toRequestBody(mediaType)

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("api-key", "$apiKey")
                .addHeader("content-type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()
            } else {
                "Request failed: ${response.code}"
            }
        } catch (e: Exception) {
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
}
