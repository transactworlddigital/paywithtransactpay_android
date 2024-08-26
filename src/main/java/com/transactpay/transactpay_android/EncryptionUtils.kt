package com.transactpay.transactpay_android

import android.util.Base64
import android.util.Log
import org.w3c.dom.Document
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.xml.parsers.DocumentBuilderFactory

object EncryptionUtils {

    private const val TAG = "EncryptionUtils"

    fun decodeBase64AndExtractKey(base64Key: String): String {
        val decodedBytes = Base64.decode(base64Key, Base64.DEFAULT)
        val decodedString = String(decodedBytes)
        val parts = decodedString.split("!")
        return parts.getOrElse(1) { "" }
    }

    fun rsaPublicKeyFromXml(xml: String): PublicKey {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docBuilderFactory.newDocumentBuilder()
        val doc: Document = docBuilder.parse(xml.byteInputStream())

        val rootElement = doc.documentElement
        val modulus = rootElement.getElementsByTagName("Modulus").item(0).textContent
        val exponent = rootElement.getElementsByTagName("Exponent").item(0).textContent

        val modulusBytes = Base64.decode(modulus, Base64.DEFAULT)
        val exponentBytes = Base64.decode(exponent, Base64.DEFAULT)

        val keySpec = RSAPublicKeySpec(
            BigInteger(1, modulusBytes),
            BigInteger(1, exponentBytes)
        )

        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    fun encryptPayloadRSA(payload: String, publicKeyXml: String): String? {
        return try {
            val pubKey = rsaPublicKeyFromXml(publicKeyXml)
            Log.d(TAG, "Public Key Generated: $pubKey")

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, pubKey)

            val encryptedBytes = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))

            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error during encryption", e)
            "Message: ${e.message}"
        }
    }
}
