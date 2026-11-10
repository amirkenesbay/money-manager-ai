package ai.moneymanager.chat.telegram.widget

import kz.rmr.chatmachinist.service.TelegramClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.util.Collections

private const val MAX_TRACKED_CARDS = 1000

@Component
class WidgetSender(
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendCard(message: SendMessage, html: String, fallback: () -> Int = { execute(message).messageId }): Int =
        runCatching { execute(SendWidget(message.chatId, html, message.replyMarkup)).messageId }
            .onFailure { log.warn("Widget send failed, falling back to text: {}", it.message) }
            .getOrElse { fallback() }
}

@Component
class WidgetMessages(
    private val outbox: ObjectProvider<WidgetOutbox>,
    private val renderer: ObjectProvider<OperationCardRenderer>,
    private val sender: ObjectProvider<WidgetSender>,
) : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
        if (bean is TelegramClient) WidgetTelegramClient(bean, outbox::getObject, renderer::getObject, sender::getObject) else bean
}

class WidgetTelegramClient(
    private val delegate: TelegramClient,
    private val outbox: () -> WidgetOutbox,
    private val renderer: () -> OperationCardRenderer,
    private val sender: () -> WidgetSender,
) : TelegramClient by delegate {
    private val replacements: MutableMap<Pair<String, Int>, Int> = Collections.synchronizedMap(
        object : LinkedHashMap<Pair<String, Int>, Int>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, Int>, Int>?): Boolean = size > MAX_TRACKED_CARDS
        }
    )

    override fun sendMessage(message: SendMessage): Int {
        val cards = outbox().take(message.chatId, message.text) ?: return delegate.sendMessage(message)
        var sentAsText = false
        val sent = sender().sendCard(message, renderer().render(cards)) { sentAsText = true; delegate.sendMessage(message) }
        if (!sentAsText) replacements[message.chatId to sent] = sent
        return sent
    }

    override fun editMessage(message: EditMessageText) {
        val key = message.chatId to (message.messageId ?: return delegate.editMessage(message))
        val target = replacements[key] ?: return delegate.editMessage(message)
        if (target != key.second) {
            message.messageId = target
            return delegate.editMessage(message)
        }
        replacements[key] = delegate.sendMessage(asNewMessage(message))
    }

    private fun asNewMessage(edit: EditMessageText): SendMessage = SendMessage().apply {
        chatId = edit.chatId
        text = edit.text
        parseMode = edit.parseMode
        disableWebPagePreview = edit.disableWebPagePreview
        replyMarkup = edit.replyMarkup
    }
}
