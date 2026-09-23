package ai.moneymanager.domain.model

data class TelegramProfile(
    val telegramUserId: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val languageCode: String?,
)
