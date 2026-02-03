package ai.moneymanager.service

import kz.rmr.chatmachinist.service.TelegramClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class NotificationSchedulerService(
    private val notificationService: NotificationService,
    private val telegramClient: TelegramClient,
    private val userInfoService: UserInfoService
) {

    /**
     * Проверка и отправка уведомлений каждые 30 секунд
     */
    @Scheduled(fixedRate = 30000)
    fun processNotifications() {
        println("⏰ [${java.time.LocalDateTime.now()}] Checking notifications...")

        val notificationsToTrigger = notificationService.getNotificationsToTrigger()

        if (notificationsToTrigger.isEmpty()) {
            println("   No notifications to trigger")
            return
        }

        println("🔔 Found ${notificationsToTrigger.size} notifications to trigger")

        notificationsToTrigger.forEach { notification ->
            try {
                // Получаем chatId пользователя (telegramUserId = chatId для личных чатов)
                val chatId = notification.userId

                // Формируем текст уведомления
                val messageText = """
                    |📣 ${notification.title}
                    |
                    |Не забудьте внести учет расходов и доходов.
                """.trimMargin()

                // Отправляем сообщение
                val message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(messageText)
                    .build()

                telegramClient.sendMessage(message)

                println("✅ Sent notification '${notification.title}' to user $chatId")

                // Обновляем nextTriggerTime
                notificationService.updateNextTriggerTime(notification.id!!)

            } catch (e: Exception) {
                println("❌ Failed to send notification '${notification.title}' to user ${notification.userId}: ${e.message}")
            }
        }
    }
}