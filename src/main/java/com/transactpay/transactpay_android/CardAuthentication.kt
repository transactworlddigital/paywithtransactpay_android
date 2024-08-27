package com.transactpay.transactpay_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.appcompat.app.AppCompatActivity
import com.transactpay.transactpay_android.BankTransfer.Companion.TAG
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


class CardAuthentication : AppCompatActivity() {

    @Volatile // This annotation ensures that changes to the flag are visible to all threads
    private var orderStatus: Boolean = true

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cardauthentication)

        val apiKey: String = intent.getStringExtra("APIKEY").toString()
        val referenceNumber: String = intent.getStringExtra("REFERRENCE").toString()
        val rsaPublicKeyXml: String = intent.getStringExtra("Encrypt").toString()
        val baseurl: String = intent.getStringExtra("BASEURL").toString()
        val success = intent.getSerializableExtra("SUCCESS") as? Class<*>
        val failed = intent.getSerializableExtra("FAILED") as? Class<*>

        Log.d(TAG, "this is the emcryptionkey : $rsaPublicKeyXml")

        val newRSA = EncryptionUtils.decodeBase64AndExtractKey(rsaPublicKeyXml)

        Log.d(TAG, "this is the new encrypt : $newRSA")

        val redirectUrl = intent.getStringExtra("Redirect_url")
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true

        Log.d(TAG, "This is the URL we are loading: $redirectUrl")

        webView.webViewClient = WebViewClient()
        redirectUrl?.let { webView.loadUrl(it) }

        val urlBank = "$baseurl/order/status"

        // Poll transaction status
        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (orderStatus) {
                    delay(10_000L) // Poll every 10 seconds

                    try {
                        val response = pollCheckTransaction(urlBank, apiKey, newRSA, referenceNumber)
                        val jObject = JSONObject(response)

                        val status = jObject.getString("status")
                        if (status == "Successful" || status == "Failed") {
                            orderStatus = false

                            withContext(Dispatchers.Main) {
                                val intent = Intent(this@CardAuthentication, if (status == "Successful") success else failed).apply {
                                    putExtra("json_data", jObject.toString())
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in Coroutine: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Coroutine: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure polling stops when the activity is destroyed
        orderStatus = false
    }

    private suspend fun pollCheckTransaction(
        statusUrl: String,
        apiKey: String,
        newRSA: String,
        referenceNumber: String
    ): String? {
        return try {
            val orderPayload = """
                {
                    "reference": "$referenceNumber"
                }
            """.trimIndent()

            val orderEncrypted = EncryptionUtils.encryptPayloadRSA(orderPayload, newRSA)
                ?: throw Exception("Encryption failed")

            val orderJson = """
                {
                    "data": "$orderEncrypted"
                }
            """.trimIndent()

            Log.d(TAG, "Order payload: $orderPayload")
            Log.d(TAG, "Public key payload: $orderEncrypted")
            Log.d(TAG, "API Order key payload: $apiKey")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val orderRequestBody: RequestBody = orderJson.toRequestBody(mediaType)

            val client = OkHttpClient()
            val requestOrder = Request.Builder()
                .url(statusUrl)
                .post(orderRequestBody)
                .addHeader("accept", "application/json")
                .addHeader("api-key", apiKey)
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
}
