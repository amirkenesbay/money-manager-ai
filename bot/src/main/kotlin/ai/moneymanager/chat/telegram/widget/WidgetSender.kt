package ai.moneymanager.chat.telegram.widget

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class WidgetSender(
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendCard(message: SendMessage, html: String, fallback: () -> Int): Int =
        runCatching { execute(SendWidget(message.chatId, html, message.replyMarkup)).messageId }
            .onFailure { log.warn("Widget send failed, falling back to text: {}", it.message) }
            .getOrElse { fallback() }
}
