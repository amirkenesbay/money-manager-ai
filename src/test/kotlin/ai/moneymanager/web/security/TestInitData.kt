package ai.moneymanager.web.security

import ai.moneymanager.domain.model.TelegramProfile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

const val TEST_BOT_TOKEN = "123456:TEST-BOT-TOKEN"

private const val HMAC_ALGORITHM = "HmacSHA256"
const val TEST_USER_ID = 42L
const val TEST_FIRST_NAME = "Amir"
const val TEST_LANGUAGE_CODE = "ru"
private const val SECRET_KEY_SEED = "WebAppData"

object TestInitData {
    fun currentTimestamp(): String = Instant.now().epochSecond.toString()

    fun secondsAgo(seconds: Long): String = (Instant.now().epochSecond - seconds).toString()

    fun valid(
        userId: Long = TEST_USER_ID,
        authDate: String = currentTimestamp(),
        botToken: String = TEST_BOT_TOKEN,
    ): String = signed(
        params = mapOf(
            "auth_date" to authDate,
            "user" to """{"id":$userId,"first_name":"$TEST_FIRST_NAME","language_code":"$TEST_LANGUAGE_CODE"}""",
        ),
        botToken = botToken,
    )

    fun profile(userId: Long = TEST_USER_ID): TelegramProfile = TelegramProfile(
        telegramUserId = userId,
        username = null,
        firstName = TEST_FIRST_NAME,
        lastName = null,
        languageCode = TEST_LANGUAGE_CODE,
    )

    fun signed(
        params: Map<String, String>,
        extraUnsignedParams: Map<String, String> = emptyMap(),
        botToken: String = TEST_BOT_TOKEN,
    ): String {
        val dataCheckString = params.toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString("\n")
        val secretKey = hmac(SECRET_KEY_SEED.toByteArray(), botToken.toByteArray())
        val hash = hmac(secretKey, dataCheckString.toByteArray()).toHex()
        return (params + extraUnsignedParams + ("hash" to hash))
            .entries
            .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    fun tamperHash(initData: String): String =
        initData.replace(Regex("hash=[0-9a-f]+"), "hash=deadbeef")

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
