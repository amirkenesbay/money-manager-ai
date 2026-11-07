package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.domain.event.SubscriptionUpdated
import ai.moneymanager.domain.model.SubscriptionChange
import ai.moneymanager.service.LocalizationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private val CHANGE_KEYS = mapOf(
    SubscriptionChange.STARTED to "subscription.started",
    SubscriptionChange.RENEWED to "subscription.renewed",
    SubscriptionChange.CANCELLED to "subscription.cancelled",
)

@Service
class SubscriptionNotifier(
    private val localizationService: LocalizationService,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun onSubscriptionUpdated(event: SubscriptionUpdated) {
        val chatId = event.user.telegramUserId ?: return
        val expiresAt = checkNotNull(event.user.subscriptionExpiresAt).toLocalDate().format(dateFormatter)
        val text = localizationService.t(checkNotNull(CHANGE_KEYS[event.subscription.change]), event.user.language, expiresAt)
        runCatching { execute(SendMessage(chatId.toString(), text)) }
            .onFailure { log.warn("Failed to tell user {} about the subscription: {}", chatId, it.message) }
    }
}
