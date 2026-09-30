package ai.moneymanager.web.security

import ai.moneymanager.domain.model.TelegramProfile

data class TelegramPrincipal(
    val userId: Long,
    val firstName: String?,
    val lastName: String?,
    val username: String?,
    val languageCode: String?,
) {
    fun toTelegramProfile(): TelegramProfile = TelegramProfile(
        telegramUserId = userId,
        username = username,
        firstName = firstName,
        lastName = lastName,
        languageCode = languageCode,
    )

    companion object {
        const val REQUEST_ATTRIBUTE = "telegramPrincipal"
    }
}
