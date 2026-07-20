package ai.moneymanager.service

import ai.moneymanager.domain.model.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private const val NEW_USER_MESSAGE_TEMPLATE = "🆕 Новый пользователь: %s (id %d)"

/** Служебные уведомления админу в alert-чат — не путать с юзер-фейсинг сообщениями бота. */
@Service
class AdminNotificationService(
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${notifications.alert-chat-id:#{null}}")
    private val alertChatId: String?
) : DefaultAbsSender(DefaultBotOptions(), botToken) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun notifyNewUser(user: UserInfo) {
        val chatId = alertChatId ?: return
        val label = user.username?.let { "@$it" }
            ?: listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { "без имени" }
        val text = NEW_USER_MESSAGE_TEMPLATE.format(label, user.telegramUserId)

        runCatching {
            execute(SendMessage(chatId, text))
        }.onFailure {
            log.warn("Failed to send new-user alert: ${it.message}")
        }
    }
}
