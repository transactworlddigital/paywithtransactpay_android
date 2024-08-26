import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.transactpay.transactpay_android.EncryptionUtils
import com.transactpay.transactpay_android.R
import com.transactpay.transactpay_android.Transactpay_start
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class PayWithTransactpay : AppCompatActivity() {

    companion object {
        private const val TAG = "ProcessingPage"

        fun newIntent(
            context: Context,
            firstName: String,
            lastName: String,
            phone: String,
            amount: String,
            email: String,
            apiKey: String,
            baseUrl: String,
            EncryptionKey: String,
            initiatingActivityClass: Class<*>,
            successClass: Class<*>,
            failureClass: Class<*>
        ): Intent {
            return Intent(context, PayWithTransactpay::class.java).apply {
                putExtra("Fname", firstName)
                putExtra("Lname", lastName)
                putExtra("Phone", phone)
                putExtra("AMOUNT", amount)
                putExtra("EMAIL", email)
                putExtra("API_KEY", apiKey)
                putExtra("BASEURL", baseUrl)
                putExtra("XMLKEY", EncryptionKey)
                putExtra("INITIATING_ACTIVITY_CLASS", initiatingActivityClass)
                putExtra("SUCCESS_CLASS", successClass)
                putExtra("FAILURE_CLASS", failureClass)
            }
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.processingpage)

        // Retrieve the data from the intent
        val fname = intent.getStringExtra("Fname") ?: ""
        val lname = intent.getStringExtra("Lname") ?: ""
        val mobile = intent.getStringExtra("Phone") ?: ""
        val amountString = intent.getStringExtra("AMOUNT") ?: "0"
        val email = intent.getStringExtra("EMAIL") ?: ""
        val apiKey = intent.getStringExtra("API_KEY") ?: ""
        val baseurl = intent.getStringExtra("BASEURL") ?: ""
        val hashKey = intent.getStringExtra("XMLKEY") ?: ""
        val initiatingClass = intent.getSerializableExtra("INITIATING_ACTIVITY_CLASS") as Class<*>
        val successClass = intent.getSerializableExtra("SUCCESS_CLASS") as Class<*>
        val failureClass = intent.getSerializableExtra("FAILURE_CLASS") as Class<*>

        Log.d(TAG, apiKey)

        // Format the amount
        val amount = amountString.toIntOrNull()
        val formattedAmount = amount?.let {
            val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
            numberFormat.maximumFractionDigits = 2
            numberFormat.minimumFractionDigits = 2
            val amountFormatted = numberFormat.format(it)
            "NGN $amountFormatted"
        } ?: "Invalid amount"

        // Generate a reference number
        val referenceNumber = generateReferenceNumber()

        val rsaPublicKeyXml = EncryptionUtils.decodeBase64AndExtractKey(hashKey)
        val url = "$baseurl/order/create"

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val response = postEncryptedPayload(
                    url, fname, lname, mobile, email, amount, referenceNumber, apiKey, rsaPublicKeyXml
                )

                val jsonResponse = JSONObject(response)

                Log.d(TAG, "HTTP Response Body: $jsonResponse")

                val status = jsonResponse.getString("status")

                if (status == "success") {
                    val data = jsonResponse.getJSONObject("data")
                    val order = data.getJSONObject("order")
                    val customer = data.getJSONObject("customer")
                    val subsidiary = data.getJSONObject("subsidiary")

                    Log.d(TAG, "First Reference $referenceNumber")

                    val intent = Intent(this@PayWithTransactpay, Transactpay_start::class.java).apply {
                        putExtra("Fname", customer.optString("firstName"))
                        putExtra("Lname", customer.optString("lastName"))
                        putExtra("Phone", customer.optString("mobile"))
                        putExtra("MERCHANT_NAME", subsidiary.optString("name"))
                        putExtra("AMOUNT", order.optString("amount"))
                        putExtra("EMAIL", customer.optString("email"))
                        putExtra("REF", referenceNumber)
                        putExtra("APIKEY", apiKey)
                        putExtra("BASEURL", baseurl)
                        putExtra("XMLKEY", hashKey)
                        putExtra("INITIATING_ACTIVITY_CLASS", initiatingClass)
                        putExtra("SUCCESS", successClass)
                        putExtra("FAILED", failureClass)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this@PayWithTransactpay, failureClass).apply {
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

    private suspend fun postEncryptedPayload(
        url: String,
        firstName: String?,
        lastName: String?,
        mobile: String?,
        email: String?,
        amount: Int?,
        ref: String,
        apiKey: String,
        publicKeyXml: String
    ): String? {
        return try {
            val payload = """
                {
                    "customer": {
                        "firstname": "$firstName",
                        "lastname": "$lastName",
                        "mobile": "$mobile",
                        "country": "NG",
                        "email": "$email"
                    },
                    "order": {
                        "amount": $amount,
                        "reference": "$ref",
                        "description": "Pay",
                        "currency": "NGN"
                    },
                    "payment": {
                        "RedirectUrl": "https://www.hi.com"
                    }
                }
            """.trimIndent()

            val encryptedData = EncryptionUtils.encryptPayloadRSA(payload, publicKeyXml)
                ?: throw Exception("Encryption failed")

            val json = """
                {
                    "data": "$encryptedData"
                }
            """.trimIndent()

            Log.d(TAG, "API KEY IS : $apiKey")
            Log.d(TAG, "Payload is : $payload")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody: RequestBody = json.toRequestBody(mediaType)

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("api-key", apiKey)
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

    private fun generateReferenceNumber(): String {
        return "REF-${System.currentTimeMillis()}"
    }
}
