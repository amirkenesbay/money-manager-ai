package ai.moneymanager.web.subscription

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGORITHM = "HmacSHA256"
private const val HEX_FORMAT = "%02x"

class TributeSignature(private val apiKey: String) {
    fun matches(body: ByteArray, signature: String?): Boolean {
        if (signature.isNullOrBlank()) return false
        val expected = sign(body).toByteArray()
        return MessageDigest.isEqual(expected, signature.trim().lowercase().toByteArray())
    }

    fun sign(body: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(apiKey.toByteArray(), HMAC_ALGORITHM))
        return mac.doFinal(body).joinToString("") { HEX_FORMAT.format(it) }
    }
}
