package com.transactpay.transactpay_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.transactpay.transactpay_android.BankTransfer.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class CardActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Retrieve the data from the intent
        val merchantName = intent.getStringExtra("MERCHANT_NAME")
        val amountString = intent.getStringExtra("AMOUNT")
        val email = intent.getStringExtra("EMAIL")
        val apiKey = intent.getStringExtra("API_KEY")
        val baseurl = intent.getStringExtra("BASEURL")
        val inititingClass = intent.getStringExtra("INITIATING_ACTIVITY_CLASS") as Class<*>
        val success = intent.getStringExtra("SUCCESS") as Class<*>
        val failed = intent.getStringExtra("FAILED") as Class<*>
        val rsaPublicKeyXml : String = intent.getStringExtra("XMLKEY").toString()
        val reff : String = intent.getStringExtra("REFERENCE_NUMBER").toString()

        Log.d(TAG, rsaPublicKeyXml)

        //encryptedKey
        val newRSA = EncryptionUtils.decodeBase64AndExtractKey(rsaPublicKeyXml)

        // Format the amount
        val amount = amountString?.toDoubleOrNull()
        val formattedAmount = amount?.let {
            // Format the amount without currency symbol
            val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.getDefault())
            numberFormat.maximumFractionDigits = 2
            numberFormat.minimumFractionDigits = 2
            val amountFormatted = numberFormat.format(it)
            // Prefix with "NGN"
            "NGN $amountFormatted"
        } ?: "Invalid amount"

        // Use the data as needed in this activity
        findViewById<TextView>(R.id.merchantNameTextView).text = merchantName
        findViewById<TextView>(R.id.amountTextView).text = formattedAmount
        val cardNumber = findViewById<EditText>(R.id.cardNumber)
        val exp = findViewById<EditText>(R.id.expirey)
        val cvv = findViewById<EditText>(R.id.cvv)

        // Limit exp input to 4 characters
        exp.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))

        var expMonth = ""
        var expYear = ""

        // Listen for text changes to split the expiry date
        exp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (it.length == 2 && !it.contains("/")) {
                        it.append("/")
                    }

                    if (it.length > 5) {
                        it.delete(5, it.length)
                    }

                    if (it.length == 5) {
                        expYear = it.substring(0, 2)
                        expMonth = it.substring(3, 5)
                    }
                }
            }
        })

        // Add TextWatcher to format the card number
        cardNumber.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    val userInput = s.toString().replace(Regex("[^\\d]"), "")
                    if (userInput.length <= 16) {
                        val formatted = userInput.chunked(4).joinToString(" ")
                        current = formatted
                        cardNumber.removeTextChangedListener(this)
                        cardNumber.setText(formatted)
                        cardNumber.setSelection(formatted.length)
                        cardNumber.addTextChangedListener(this)
                    }
                }
            }
        })

        val payButton: Button = findViewById(R.id.payButton)
        findViewById<TextView>(R.id.emailTextView).text = email

        payButton.text = "Pay $formattedAmount"
        payButton.setOnClickListener {
            val url = "$baseurl/order/pay"
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = postEncryptedPayload(url, cardNumber.text.toString(), expMonth, expYear, cvv.text.toString(), newRSA, reff, apiKey.toString())
                    // Attempt to parse the response string as JSON
                    val jsonResponse = JSONObject(response)
                    // Log the actual response body content
                    Log.d(TAG, "HTTP Response Body: $jsonResponse")

                    // Extract status and data
                    val status = jsonResponse.getString("status")
//                  val data = jsonResponse.getJSONObject("data")

                    Log.d(TAG, "Final status is : $status")

                        if (status == "success") {
                            Log.d(TAG, "API Call Success: $jsonResponse")
                        } else {
                            //redirect to failed page
                            val intent = Intent(this@CardActivity, failed).apply {
                                putExtra("status", jsonResponse.getString("status"))
                                putExtra("code", jsonResponse.getString("statusCode"))
                                putExtra("message", jsonResponse.getString("message"))
                            }
                            startActivity(intent)
                        }
                }
            } catch (e: Exception) {
                Log.d(TAG, "This is the error $e")
            }
        }

        // Back to options icon and click
        val backToOptions: ImageView = findViewById(R.id.backToOptions)
        backToOptions.setOnClickListener {
            val intent = Intent(this, Transactpay_start::class.java).apply {
                putExtra("MERCHANT_NAME", merchantName)
                putExtra("AMOUNT", amountString)
                putExtra("EMAIL", email)
                putExtra("API_KEY", apiKey)
            }
            startActivity(intent)
        }
    }

    private suspend fun postEncryptedPayload(
        url: String,
        cardNumber: String,
        expmonth: String,
        expYear: String,
        cvv2: String,
        newRSA: String,
        ref: String,
        apiKey : String
    ): String? {
        return try {
            val payload = """
                {
                    "reference": "$ref",
                    "paymentoption": "C",
                    "country": "NG",
                    "card": {
                        "cardnumber": "$cardNumber",
                        "expirymonth": "$expmonth",
                        "expiryyear": "$expYear",
                        "cvv": "$cvv2"
                    }
                }
            """.trimIndent()

            Log.d(TAG, payload)

            val encryptedData = EncryptionUtils.encryptPayloadRSA(payload, newRSA) ?: throw Exception("Encryption failed")
            Log.d(TAG, encryptedData)
            val json = """
                {
                    "data": "$encryptedData"
                }
            """.trimIndent()

            Log.d(TAG, json)

            Log.d(TAG, "The Api Key Is: $apiKey")

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
                response.body?.string()
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
