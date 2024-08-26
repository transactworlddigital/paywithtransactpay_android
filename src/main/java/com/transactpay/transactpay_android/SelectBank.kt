package com.transactpay.transactpay_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class SelectBank : AppCompatActivity() {

    private lateinit var bankDropdown: Spinner
    private var bankName: String? = null
    private var bankCode: String? = null
    private var recipientAccount: String? = null

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.selectbank)

        val fname = intent.getStringExtra("Fname")
        val lname = intent.getStringExtra("Lname")
        val phone = intent.getStringExtra("Phone")
        val merchantName = intent.getStringExtra("MERCHANT_NAME")
        val amountString = intent.getStringExtra("AMOUNT")
        val email = intent.getStringExtra("EMAIL")
        val apiKey = intent.getStringExtra("API_KEY")
        val baseurl = intent.getStringExtra("BASEURL")
        val inititingClass = intent.getStringExtra("INITIATING_ACTIVITY_CLASS") as Class<*>
        val success = intent.getStringExtra("SUCCESS") as Class<*>
        val failed = intent.getStringExtra("FAILED") as Class<*>
        val encryptKey: String = intent.getStringExtra("XMLKEY").toString()
        val referenceNumber: String = intent.getStringExtra("REFERENCE_NUMBER").toString()

        val amount = amountString?.toDoubleOrNull()
        val formattedAmount = amount?.let {
            val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.getDefault())
            numberFormat.maximumFractionDigits = 2
            numberFormat.minimumFractionDigits = 2
            val amountFormatted = numberFormat.format(it)
            "NGN $amountFormatted"
        } ?: "Invalid amount"

        findViewById<TextView>(R.id.merchantNameTextView).text = merchantName
        findViewById<TextView>(R.id.amountTextView).text = formattedAmount
        findViewById<TextView>(R.id.emailTextView).text = email

        // URL for fetching banks data
        val url = "$baseurl/banks?paymentmethod=banktransfer"

        val rsaPublicKeyXml = com.transactpay.transactpay_android.EncryptionUtils.decodeBase64AndExtractKey(encryptKey)
        val urlBank = "$baseurl/order/pay"

        // Find the Spinner in the layout
        bankDropdown = findViewById(R.id.bankDropdown)

        // Fetch and populate the Spinner with bank data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = postEncryptedPayload(url, apiKey.toString())
                response?.let {
                    // Parse the JSON response and update the Spinner on the main thread
                    val bankList = parseBankJson(it)
                    withContext(Dispatchers.Main) {
                        updateSpinner(bankList)
                    }
                }

                val responseBankAcc = responseBankAcc(urlBank, apiKey.toString(), rsaPublicKeyXml, referenceNumber, bankCode.toString())
                val jsonResponse = JSONObject(responseBankAcc)

                recipientAccount = jsonResponse.getJSONObject("data")
                    .getJSONObject("paymentDetail")
                    .getString("recipientAccount")

            } catch (e: Exception) {
                Log.e(TAG, "Error in Coroutine: ${e.message}")
            }
        }

        // Handle make payment
        findViewById<Button>(R.id.makePayment).setOnClickListener {

            Log.d(TAG, "local amount is : $amount")


            val intent = Intent(this@SelectBank, BankTransfer::class.java).apply {
                putExtra("Fname", fname)
                putExtra("Lname", lname)
                putExtra("Phone", phone)
                putExtra("MERCHANT_NAME", merchantName)
                putExtra("AMOUNT", amount.toString())
                putExtra("EMAIL", email)
                putExtra("API_KEY", apiKey)
                putExtra("XMLKEY", encryptKey)
                putExtra("REFERENCE_NUMBER", referenceNumber)
                putExtra("BankName", bankName)
                putExtra("BankCode", bankCode)
                putExtra("BASEURL", baseurl)
                putExtra("INITIATING_ACTIVITY_CLASS", inititingClass)
                putExtra("SUCCESS", success)
                putExtra("FAILED", failed)
                putExtra("RecipientAccount", recipientAccount)
            }
            startActivity(intent)
        }
    }

    private fun parseBankJson(jsonString: String): List<com.transactpay.transactpay_android.Bank> {
        val bankList = mutableListOf<com.transactpay.transactpay_android.Bank>()
        try {
            val jsonObject = JSONObject(jsonString)
            val dataArray: JSONArray = jsonObject.getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val bankObject = dataArray.getJSONObject(i)
                val bank = com.transactpay.transactpay_android.Bank(
                    name = bankObject.getString("name"),
                    logoUrl = bankObject.getString("logo")
                )
                bankList.add(bank)

                // Extract the bankCode and bankName from the first item for the intent
                if (i == 0) {
                    bankName = bankObject.getString("name")
                    bankCode = bankObject.getString("bankCode")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
        }
        return bankList
    }

    private fun updateSpinner(bankList: List<com.transactpay.transactpay_android.Bank>) {
        // Set the custom adapter with the fetched data
        val adapter = com.transactpay.transactpay_android.BankSpinnerAdapter(this, bankList)
        bankDropdown.adapter = adapter
    }

    private suspend fun postEncryptedPayload(
        url: String,
        apiKey: String,
    ): String? {
        return try {
            Log.d(TAG, "API KEY IS : $apiKey")

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("api-key", apiKey)
                .addHeader("content-type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Account Body: $responseBody")

            if (response.isSuccessful) {
                responseBody
            } else {
                Log.e(TAG, "Response unsuccessful: ${response.message}")
                responseBody
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during postEncryptedPayload: ${e.message}")
            "Exception: ${e.message}"
        }
    }

    private suspend fun responseBankAcc(
        url: String,
        apiKey: String,
        publicKeyXml: String,
        reference: String,
        bankCode : String
    ): String? {
        return try {
            val payload = """
                {
                    "reference": "$reference",
                    "paymentoption": "bank-transfer",
                    "country": "NG",
                    "BankTransfer": {
                        "bankcode": "$bankCode"
                    }
                }
            """.trimIndent()

            val encryptedData = com.transactpay.transactpay_android.EncryptionUtils.encryptPayloadRSA(payload, publicKeyXml) ?: throw Exception("Encryption failed")

            val json = """
                {
                    "data": "$encryptedData"
                }
            """.trimIndent()

            Log.d(com.transactpay.transactpay_android.BankTransfer.TAG, "API KEY IS : $apiKey")
            Log.d(com.transactpay.transactpay_android.BankTransfer.TAG, "Payload is : $payload")

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
            val responseBody = response.body?.string()

            Log.d(com.transactpay.transactpay_android.BankTransfer.TAG, "Account Body: $responseBody")

            if (response.isSuccessful) {
                responseBody
            } else {
                Log.e(com.transactpay.transactpay_android.BankTransfer.TAG, "Response unsuccessful: ${response.message}")
                responseBody
            }
        } catch (e: Exception) {
            Log.e(com.transactpay.transactpay_android.BankTransfer.TAG, "Exception during responseBankAcc: ${e.message}")
            "Exception: ${e.message}"
        }
    }

    companion object {
        private const val TAG = "SelectBank"
    }
}
