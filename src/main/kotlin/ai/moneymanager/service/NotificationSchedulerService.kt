package ai.moneymanager.service

import ai.moneymanager.repository.entity.NotificationEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.LocalDateTime

@Service
class NotificationSchedulerService(
    private val notificationService: NotificationService,
    private val userInfoService: UserInfoService,
    @Value("\${chat-machinist.bot.token}")
    private val botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String
) : DefaultAbsSender(
    DefaultBotOptions(), botToken
) {

    private val log = LoggerFactory.getLogger(NotificationSchedulerService::class.java)

    @Scheduled(fixedRate = 60000)
    fun checkAndSendNotifications() {
        val now = LocalDateTime.now()
        val dueNotifications = notificationService.getDueNotifications(now)

        if (dueNotifications.isEmpty()) return

        log.info("Found {} due notifications", dueNotifications.size)

        for (notification in dueNotifications) {
            try {
                sendNotification(notification)
                val userInfo = userInfoService.getUserInfoByTelegramId(notification.telegramUserId)
                val timezone = userInfo?.timezone ?: "UTC"
                notificationService.advanceNextFireTime(notification, timezone)
                log.info("Sent and advanced notification '{}' for user {}", notification.name, notification.telegramUserId)
            } catch (e: Exception) {
                log.error("Failed to send notification '{}' to user {}: {}", notification.name, notification.telegramUserId, e.message, e)
            }
        }
    }

    private fun sendNotification(notification: NotificationEntity) {
        val messageText = """
            |${notification.icon ?: "🔔"} ${notification.name}
            |
            |Не забудьте внести учёт расходов и доходов!
        """.trimMargin()

        val keyboard = InlineKeyboardMarkup().apply {
            keyboard = listOf(
                listOf(
                    InlineKeyboardButton("📊 Открыть Финансы").apply {
                        url = "https://t.me/$botUsername"
                    }
                )
            )
        }

        val sendMessage = SendMessage().apply {
            chatId = notification.telegramUserId.toString()
            text = messageText
            replyMarkup = keyboard
        }

        execute(sendMessage)
    }
}
