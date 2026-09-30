package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.TelegramProfile
import org.telegram.telegrambots.meta.api.objects.User

fun User.toTelegramProfile(): TelegramProfile = TelegramProfile(
    telegramUserId = id,
    username = userName,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
)
