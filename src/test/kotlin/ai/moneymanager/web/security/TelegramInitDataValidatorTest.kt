package ai.moneymanager.web.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TelegramInitDataValidatorTest {

    private val botToken = "123456:TEST-BOT-TOKEN"
    private val validator = TelegramInitDataValidator(botToken, ObjectMapper())

    @Test
    fun `returns principal for correctly signed initData`() {
        val initData = signedInitData(
            mapOf(
                "auth_date" to now(),
                "user" to """{"id":42,"first_name":"Amir","language_code":"ru"}""",
            ),
        )

        val principal = requireNotNull(validator.validate(initData)) {
            "expected correctly signed initData to be accepted"
        }

        assertThat(principal.userId).isEqualTo(42L)
        assertThat(principal.firstName).isEqualTo("Amir")
        assertThat(principal.languageCode).isEqualTo("ru")
        assertThat(principal.lastName).isNull()
    }

    @Test
    fun `returns null when hash is tampered`() {
        val valid = signedInitData(
            mapOf(
                "auth_date" to now(),
                "user" to """{"id":42,"first_name":"Amir"}""",
            ),
        )
        val tampered = valid.replace(Regex("hash=[0-9a-f]+"), "hash=deadbeef")

        assertThat(validator.validate(tampered)).isNull()
    }

    @Test
    fun `returns null when hash is missing`() {
        assertThat(validator.validate("auth_date=${now()}&user=%7B%7D")).isNull()
    }

    @Test
    fun `returns null when auth_date is too old`() {
        val stale = signedInitData(
            mapOf(
                "auth_date" to (Instant.now().epochSecond - 100_000).toString(),
                "user" to """{"id":42,"first_name":"Amir"}""",
            ),
        )

        assertThat(validator.validate(stale)).isNull()
    }

    @Test
    fun `ignores signature field when computing hash`() {
        val initData = signedInitData(
            params = mapOf(
                "auth_date" to now(),
                "user" to """{"id":42,"first_name":"Amir"}""",
            ),
            extraUnsignedParams = mapOf("signature" to "ed25519-signature-not-checked"),
        )

        assertThat(validator.validate(initData)).isNotNull
    }

    private fun now(): String = Instant.now().epochSecond.toString()

    /**
     * Builds an initData query string signed exactly as Telegram does:
     * HMAC-SHA256 over the sorted `key=value` lines, keyed by HMAC("WebAppData", botToken).
     * [extraUnsignedParams] are appended to the query but excluded from the signature.
     */
    private fun signedInitData(
        params: Map<String, String>,
        extraUnsignedParams: Map<String, String> = emptyMap(),
    ): String {
        val dataCheckString = params.toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString("\n")
        val secretKey = hmac("WebAppData".toByteArray(), botToken.toByteArray())
        val hash = hmac(secretKey, dataCheckString.toByteArray()).toHex()
        return (params + extraUnsignedParams + ("hash" to hash))
            .entries
            .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
