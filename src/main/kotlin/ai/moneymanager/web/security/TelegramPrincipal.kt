package ai.moneymanager.web.security

/**
 * Authenticated Telegram user, extracted from a validated Mini App `initData` string.
 */
data class TelegramPrincipal(
    val userId: Long,
    val firstName: String?,
    val lastName: String?,
    val username: String?,
    val languageCode: String?,
) {
    companion object {
        const val REQUEST_ATTRIBUTE = "telegramPrincipal"
    }
}
