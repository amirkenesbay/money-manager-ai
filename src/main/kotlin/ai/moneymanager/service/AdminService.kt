package ai.moneymanager.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class AdminService(
    @Value("\${admin.telegram-user-ids:}")
    adminIdsRaw: String,
    @Value("\${chat-machinist.bot.token}")
    botToken: String
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val adminIds: Set<Long> = adminIdsRaw
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }
        .toSet()

    fun isAdmin(telegramUserId: Long): Boolean = telegramUserId in adminIds

    fun reply(chatId: Long, text: String) {
        runCatching {
            execute(SendMessage(chatId.toString(), text))
        }.onFailure {
            log.warn("Failed to send admin command reply: ${it.message}")
        }
    }
}
