package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.event.SubscriptionUpdated
import ai.moneymanager.domain.model.formatUserHandle
import ai.moneymanager.domain.model.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private const val NEW_USER_MESSAGE_TEMPLATE = "🆕 Новый пользователь: %s, %s (id %d)"
private const val NO_USERNAME_PLACEHOLDER = "без username"
private const val NO_NAME_PLACEHOLDER = "без имени"
private const val SUBSCRIPTION_MESSAGE_TEMPLATE = "💰 Tribute: %s — %s, %s %s %s, до %s"

@Service
class AdminNotificationService(
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${notifications.alert-chat-id:#{null}}")
    private val alertChatId: String?
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun onNewUserRegistered(event: NewUserRegistered) {
        notifyNewUser(event.user)
    }

    @EventListener
    fun onSubscriptionUpdated(event: SubscriptionUpdated) {
        val subscription = event.subscription
        val text = SUBSCRIPTION_MESSAGE_TEMPLATE.format(
            formatUserHandle(event.user), subscription.change, subscription.period ?: "", subscription.price, subscription.currency, event.user.subscriptionExpiresAt?.toLocalDate(),
        )
        send(text)
    }

    private fun send(text: String) {
        val chatId = alertChatId ?: return
        runCatching { execute(SendMessage(chatId, text)) }.onFailure { log.warn("Failed to send admin alert: ${it.message}") }
    }

    private fun notifyNewUser(user: UserInfo) {
        val username = user.username?.let { "@$it" } ?: NO_USERNAME_PLACEHOLDER
        val fullName = listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { NO_NAME_PLACEHOLDER }
        send(NEW_USER_MESSAGE_TEMPLATE.format(username, fullName, user.telegramUserId))
    }
}
