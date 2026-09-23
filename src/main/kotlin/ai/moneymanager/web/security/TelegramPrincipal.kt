package ai.moneymanager.web.security

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
