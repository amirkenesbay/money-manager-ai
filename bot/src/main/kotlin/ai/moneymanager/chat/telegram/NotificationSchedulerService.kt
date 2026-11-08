package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.FALLBACK_TIMEZONE
import ai.moneymanager.domain.model.UserInfo
import io.micrometer.core.instrument.MeterRegistry
import ai.moneymanager.domain.model.DEFAULT_NOTIFICATION_ICON
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.domain.model.StartParameters
import ai.moneymanager.repository.entity.NotificationEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.LocalDateTime

private const val ADD_OPERATION_BUTTON_KEY = "mini_app.button.add_operation"
private const val REMINDERS_METRIC = "moneymanager.reminders.delivered"
private const val OUTCOME_TAG = "outcome"
private const val OUTCOME_SENT = "sent"
private const val OUTCOME_FAILED = "failed"
private const val OUTCOME_SKIPPED = "skipped"
private const val BOT_BLOCKED_STATUS = 403


@Service
class NotificationSchedulerService(
    private val notificationService: NotificationService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    private val meterRegistry: MeterRegistry,
    @Value("\${chat-machinist.bot.token}")
    private val botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String
) : DefaultAbsSender(
    DefaultBotOptions(), botToken
) {
    private val log = LoggerFactory.getLogger(NotificationSchedulerService::class.java)

    init {
        listOf(OUTCOME_SENT, OUTCOME_FAILED, OUTCOME_SKIPPED).forEach { meterRegistry.counter(REMINDERS_METRIC, OUTCOME_TAG, it) }
    }

    @Scheduled(fixedRate = 60000)
    fun checkAndSendNotifications() {
        val now = LocalDateTime.now()
        val dueNotifications = notificationService.getDueNotifications(now)

        if (dueNotifications.isEmpty()) return

        log.info("Found {} due notifications", dueNotifications.size)

        for (notification in dueNotifications) {
            val userInfo = userInfoService.getUserInfoByTelegramId(notification.telegramUserId)
            val outcome = deliver(notification, userInfo)
            runCatching { notificationService.advanceNextFireTime(notification, userInfo?.timezone ?: FALLBACK_TIMEZONE) }
                .onFailure { log.error("Failed to schedule notification '{}' of user {}: {}", notification.name, notification.telegramUserId, it.message, it) }
            meterRegistry.counter(REMINDERS_METRIC, OUTCOME_TAG, outcome).increment()
        }
    }

    private fun deliver(notification: NotificationEntity, userInfo: UserInfo?): String {
        if (userInfo?.blocked == true) return OUTCOME_SKIPPED
        return runCatching { sendNotification(notification, userInfo?.language) }
            .fold(
                onSuccess = { OUTCOME_SENT },
                onFailure = {
                    log.warn("Failed to send notification '{}' to user {}: {}", notification.name, notification.telegramUserId, it.message)
                    if (it.isBotBlocked()) userInfoService.markBotBlocked(notification.telegramUserId, true)
                    OUTCOME_FAILED
                },
            )
    }

    private fun Throwable.isBotBlocked(): Boolean = this is TelegramApiRequestException && errorCode == BOT_BLOCKED_STATUS

    private fun sendNotification(notification: NotificationEntity, language: String?) {
        val icon = notification.icon ?: DEFAULT_NOTIFICATION_ICON
        val messageText = localizationService.t("notification.push.body", language, icon, notification.name)

        val sendMessage = SendMessage().apply {
            chatId = notification.telegramUserId.toString()
            text = messageText
            replyMarkup = reminderKeyboard(language)
        }

        execute(sendMessage)
    }

    private fun reminderKeyboard(language: String?): InlineKeyboardMarkup {
        if (miniAppGateway.enabled) {
            return miniAppGateway.keyboard(ADD_OPERATION_BUTTON_KEY, language, MiniAppScreen.ADD_OPERATION)
        }
        val openFinanceText = localizationService.t("notification.push.button.open_finance", language)
        val button = InlineKeyboardButton(openFinanceText).apply {
            url = "https://t.me/$botUsername?start=${StartParameters.OPEN_FINANCE}"
        }
        return InlineKeyboardMarkup(listOf(listOf(button)))
    }
}
