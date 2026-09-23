package ai.moneymanager.web.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URLDecoder
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private object InitDataField {
    const val HASH = "hash"
    const val SIGNATURE = "signature"
    const val USER = "user"
    const val AUTH_DATE = "auth_date"
}

private object UserField {
    const val ID = "id"
    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val USERNAME = "username"
    const val LANGUAGE_CODE = "language_code"
}

private val SIGNATURE_FIELDS = setOf(InitDataField.HASH, InitDataField.SIGNATURE)

private const val HMAC_ALGORITHM = "HmacSHA256"

private const val SECRET_KEY_SEED = "WebAppData"

private const val INIT_DATA_TTL_SECONDS = 86_400L

private const val DATA_CHECK_LINE_SEPARATOR = "\n"

private const val PARAM_SEPARATOR = '&'

private const val KEY_VALUE_DELIMITER = '='

@Component
class TelegramInitDataValidator(
    @param:Value("\${chat-machinist.bot.token}") private val botToken: String,
    private val jsonMapper: JsonMapper,
) {
    fun validate(initData: String): TelegramPrincipal? {
        val params = parseQuery(initData)
        val receivedHash = params[InitDataField.HASH] ?: return null
        val authDate = params[InitDataField.AUTH_DATE]?.toLongOrNull() ?: return null
        if (isExpired(authDate)) return null
        if (!signatureMatches(params, receivedHash)) return null
        return params[InitDataField.USER]?.let(::parseUser)
    }

    private fun signatureMatches(params: Map<String, String>, receivedHash: String): Boolean {
        val expectedHash = computeHash(params)
        return MessageDigest.isEqual(expectedHash.toByteArray(), receivedHash.toByteArray())
    }

    private fun computeHash(params: Map<String, String>): String {
        val dataCheckString = buildDataCheckString(params)
        val secretKey = hmacSha256(key = SECRET_KEY_SEED.toByteArray(), message = botToken.toByteArray())
        return hmacSha256(key = secretKey, message = dataCheckString.toByteArray()).toHex()
    }

    private fun buildDataCheckString(params: Map<String, String>): String =
        params
            .filterKeys { key -> key !in SIGNATURE_FIELDS }
            .toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString(DATA_CHECK_LINE_SEPARATOR)

    private fun isExpired(authDate: Long): Boolean =
        Instant.now().epochSecond - authDate > INIT_DATA_TTL_SECONDS

    private fun parseUser(userJson: String): TelegramPrincipal? {
        val user = jsonMapper.readTree(userJson)
        val userId = user.text(UserField.ID)?.toLongOrNull() ?: return null
        return TelegramPrincipal(
            userId = userId,
            firstName = user.text(UserField.FIRST_NAME),
            lastName = user.text(UserField.LAST_NAME),
            username = user.text(UserField.USERNAME),
            languageCode = user.text(UserField.LANGUAGE_CODE),
        )
    }

    private fun JsonNode.text(field: String): String? =
        get(field)?.takeUnless { it.isNull }?.asString()

    private fun parseQuery(query: String): Map<String, String> =
        query.split(PARAM_SEPARATOR)
            .filter { pair -> pair.contains(KEY_VALUE_DELIMITER) }
            .associate(::parseQueryPair)

    private fun parseQueryPair(pair: String): Pair<String, String> {
        val rawKey = pair.substringBefore(KEY_VALUE_DELIMITER)
        val rawValue = pair.substringAfter(KEY_VALUE_DELIMITER)
        return decode(rawKey) to decode(rawValue)
    }

    private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8)

    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(message)
    }

    private fun ByteArray.toHex(): String = HexFormat.of().formatHex(this)
}
