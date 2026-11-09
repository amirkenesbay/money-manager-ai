package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.metrics.MessageMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.RecordedOccurrence
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import ai.moneymanager.testing.testUser
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val BOT_TOKEN = "123456:TEST"
private const val BOT_NAME = "testBot"
private val TODAY: LocalDate = LocalDate.of(2026, 11, 15)

class RecurringNotifierTest {
    private val group = testGroup()
    private val service = mock(RecurringPaymentService::class.java)
    private val userInfoService = mock(UserInfoService::class.java)
    private val groupService = mock(GroupService::class.java)

    @BeforeEach
    fun givenUserAndGroup() {
        `when`(userInfoService.getUserInfoByTelegramId(TEST_USER_ID)).thenReturn(testUser())
        `when`(groupService.getGroup(checkNotNull(group.id))).thenReturn(group)
    }

    @Test
    fun `records a due automatic payment in the morning and offers to undo it`() {
        val payment = payment(RecurringMode.AUTO, TODAY)
        val operation = operation()
        `when`(service.dueBy(TODAY.plusDays(2))).thenReturn(listOf(payment))
        `when`(service.recordDue(payment, TODAY)).thenReturn(listOf(RecordedOccurrence(TODAY, operation)))
        val notifier = notifierAt(TODAY.atTime(10, 5))

        notifier.processDuePayments()

        val message = sent(notifier).single()
        assertThat(message.text).contains("📺 Подписки · Netflix", "−3 500")
        assertThat((message.replyMarkup as InlineKeyboardMarkup).keyboard.single().single().url)
            .isEqualTo(startLink(BOT_NAME, undoOperationParameter(checkNotNull(operation.id))))
    }

    @Test
    fun `waits until the morning`() {
        val payment = payment(RecurringMode.AUTO, TODAY)
        `when`(service.dueBy(TODAY.plusDays(2))).thenReturn(listOf(payment))

        notifierAt(TODAY.atTime(7, 0)).processDuePayments()

        verify(service, never()).recordDue(payment, TODAY)
    }

    @Test
    fun `warns in the evening before the payment`() {
        val payment = payment(RecurringMode.REMIND, TODAY.plusDays(1))
        `when`(service.dueBy(TODAY.plusDays(2))).thenReturn(listOf(payment))
        `when`(service.claimHeadsUp(payment, TODAY.plusDays(1))).thenReturn(true)
        val notifier = notifierAt(TODAY.atTime(19, 30))

        notifier.processDuePayments()

        assertThat(sent(notifier).single().text).contains("Netflix", "напомню")
    }

    private fun notifierAt(now: LocalDateTime): RecurringNotifier {
        val clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        val notifier = spy(RecurringNotifier(service, userInfoService, groupService, testLocalizationService(), MessageMetrics(SimpleMeterRegistry()), clock, BOT_TOKEN, BOT_NAME))
        doReturn(Message()).`when`(notifier).execute(any(SendMessage::class.java))
        return notifier
    }

    private fun sent(notifier: RecurringNotifier): List<SendMessage> =
        mockingDetails(notifier).invocations.filter { it.method.name == "execute" }.map { it.arguments.single() as SendMessage }

    private fun payment(mode: RecurringMode, nextDate: LocalDate) = RecurringPaymentEntity(
        id = ObjectId(), groupId = checkNotNull(group.id), creatorId = TEST_USER_ID, type = CategoryType.EXPENSE, amount = BigDecimal("3500"),
        categoryId = ObjectId(), categoryName = "Подписки", categoryIcon = "📺", description = "Netflix",
        unit = RecurrenceUnit.MONTH, interval = 1, startDate = TODAY.minusMonths(1), endDate = null, nextDate = nextDate, mode = mode,
    )

    private fun operation() = FinanceOperationEntity(
        id = ObjectId(), groupId = checkNotNull(group.id), creatorId = TEST_USER_ID, type = CategoryType.EXPENSE, amount = BigDecimal("3500"),
        categoryId = ObjectId(), categoryName = "Подписки", categoryIcon = "📺", operationDate = TODAY, description = "Netflix",
    )
}
