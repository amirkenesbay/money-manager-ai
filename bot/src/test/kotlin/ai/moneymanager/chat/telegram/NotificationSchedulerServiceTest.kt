package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.metrics.MessageMetrics
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.DailyReminderService
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.SpendingReminderService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testLocalizationService
import ai.moneymanager.testing.testUser
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import java.time.LocalDateTime

private const val APP_URL = "https://moneymanager.kz/app"
private const val BOT_TOKEN = "123:test"
private const val BOT_NAME = "test_bot"
private const val MESSAGES = "moneymanager.messages"

class NotificationSchedulerServiceTest {
    private val localization = testLocalizationService()
    private val notificationService = mock(NotificationService::class.java)
    private val userInfoService = mock(UserInfoService::class.java)
    private val spendingReminderService = mock(SpendingReminderService::class.java)
    private val dailyReminderService = DailyReminderService(userInfoService, notificationService, localization, mock(ai.moneymanager.service.SubscriptionLimitsService::class.java))
    private val registry = SimpleMeterRegistry()
    private val groupId = ObjectId()
    private val coffee = Category(ObjectId(), "Кофе", "☕", CategoryType.EXPENSE, groupId)
    private val user = testUser(language = "ru", activeGroupId = groupId)
    private val scheduler = spy(
        NotificationSchedulerService(
            notificationService, userInfoService, localization, gateway(), dailyReminderService, spendingReminderService,
            MessageMetrics(registry), registry, BOT_TOKEN, BOT_NAME,
        )
    )

    private val spendingReminder = NotificationEntity(
        telegramUserId = TEST_USER_ID,
        name = localization.t("notification.default.daily_spending", "ru"),
        icon = "💸",
        frequencyType = FrequencyType.DAILY,
        hour = 21,
        minute = 0,
        nextFireTime = LocalDateTime.now().minusMinutes(1),
    )

    @BeforeEach
    fun givenDueReminder() {
        `when`(notificationService.getDueNotifications(any(LocalDateTime::class.java) ?: LocalDateTime.MIN)).thenReturn(listOf(spendingReminder))
        `when`(userInfoService.getUserInfoByTelegramId(TEST_USER_ID)).thenReturn(user)
        `when`(spendingReminderService.quickCategories(user)).thenReturn(listOf(coffee))
        doReturn(Message()).`when`(scheduler).execute(any(SendMessage::class.java))
    }

    @Test
    fun `stays quiet when the user already recorded something today`() {
        `when`(spendingReminderService.recordedToday(user)).thenReturn(true)

        scheduler.checkAndSendNotifications()

        verify(scheduler, never()).execute(any(SendMessage::class.java))
        assertThat(count("spending_reminder", "skipped")).isEqualTo(1.0)
    }

    @Test
    fun `asks about an empty day with one-tap buttons for the usual categories`() {
        `when`(spendingReminderService.recordedToday(user)).thenReturn(false)

        scheduler.checkAndSendNotifications()

        val sent = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(scheduler).execute(sent.capture())
        assertThat(sent.value.text).isEqualTo(localization.t("notification.spending.empty", "ru"))
        val firstButton = (sent.value.replyMarkup as InlineKeyboardMarkup).keyboard.first().first()
        assertThat(firstButton.text).isEqualTo("☕ Кофе")
        assertThat(firstButton.webApp.url).isEqualTo("$APP_URL?screen=add&category=${coffee.id!!.toHexString()}&src=spending_reminder")
        assertThat(count("spending_reminder", "sent")).isEqualTo(1.0)
    }

    private fun count(type: String, outcome: String): Double = registry.counter(MESSAGES, "type", type, "outcome", outcome).count()

    private fun gateway(): MiniAppGateway {
        val properties = MiniAppProperties().apply {
            url = APP_URL
            botGateway = true
        }
        return MiniAppGateway(properties, userInfoService, localization, BOT_TOKEN)
    }
}
