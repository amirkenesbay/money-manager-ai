package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.group.GroupNoticeFormatter
import ai.moneymanager.domain.event.CategoryCreated
import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.event.OperationChanged
import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.GroupNoticeLevel
import ai.moneymanager.domain.model.monthPeriod
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.BudgetAlert
import ai.moneymanager.service.BudgetAlertService
import ai.moneymanager.service.GroupService
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
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

private const val BOT_TOKEN = "123456:TEST"
private const val PARTNER = TEST_USER_ID + 1
private const val QUIET_MEMBER = TEST_USER_ID + 2
private val START: Instant = Instant.parse("2026-11-04T10:00:00Z")

class GroupActivityNotifierTest {
    private val groupId = ObjectId()
    private val group = testGroup(groupId, members = setOf(TEST_USER_ID, PARTNER, QUIET_MEMBER))
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId, BigDecimal("20000"))
    private val groupService = mock(GroupService::class.java)
    private val budgetAlertService = mock(BudgetAlertService::class.java)
    private val userInfoService = mock(UserInfoService::class.java)
    private val clock = MovableClock(START)
    private val localization = testLocalizationService()
    private val notifier = spy(
        GroupActivityNotifier(
            groupService, budgetAlertService, userInfoService, localization,
            MiniAppGateway(MiniAppProperties(), userInfoService, localization, BOT_TOKEN),
            GroupNoticeFormatter(localization), OperationNoticeBuffer(clock), BOT_TOKEN,
        )
    )

    @BeforeEach
    fun givenGroupWithAQuietMember() {
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(userInfoService.getUserInfoByTelegramId(TEST_USER_ID)).thenReturn(testUser())
        `when`(userInfoService.getUserInfoByTelegramId(PARTNER)).thenReturn(testUser().copy(telegramUserId = PARTNER))
        `when`(userInfoService.getUserInfoByTelegramId(QUIET_MEMBER))
            .thenReturn(testUser().copy(telegramUserId = QUIET_MEMBER, groupNotices = mapOf(groupId.toHexString() to GroupNoticeLevel.OFF)))
        `when`(userInfoService.displayNames(setOf(TEST_USER_ID))).thenReturn(mapOf(TEST_USER_ID to "Аня"))
        doReturn(Message()).`when`(notifier).execute(any(SendMessage::class.java))
    }

    @Test
    fun `warns members who keep notices on that a budget is almost spent`() {
        val budget = Budget(taxi, BigDecimal("20000"), BigDecimal("17000"), monthPeriod(YearMonth.of(2026, 10), 1))
        `when`(budgetAlertService.claimNewAlerts(group)).thenReturn(listOf(BudgetAlert(budget, BudgetThreshold.NEAR)))

        notifier.onOperationsChanged(OperationsChanged(groupId))

        assertThat(sent().map { it.chatId }).containsExactlyInAnyOrder(TEST_USER_ID.toString(), PARTNER.toString())
        assertThat(sent().first().text).contains("🚕 Такси", "85%", "17 000", "20 000", "31.10", "3 000")
    }

    @Test
    fun `tells the others right away who added a category`() {
        notifier.onCategoryCreated(CategoryCreated(groupId, TEST_USER_ID, "☕", "Кафе"))

        assertThat(sent().map { it.chatId }).containsExactly(PARTNER.toString())
        assertThat(sent().single().text).contains("Аня", "☕ Кафе")
    }

    @Test
    fun `holds operations until the author goes quiet and sends them as one notice`() {
        notifier.onOperationChanged(OperationChanged(groupId, TEST_USER_ID, OperationChange.Added(operation("8450", "Магнум"))))
        clock.advanceMinutes(1)
        notifier.onOperationChanged(OperationChanged(groupId, TEST_USER_ID, OperationChange.Added(operation("1900", null))))
        clock.advanceMinutes(1)
        notifier.flushOperationNotices()
        assertThat(sent()).isEmpty()

        clock.advanceMinutes(1)
        notifier.flushOperationNotices()

        assertThat(sent().map { it.chatId }).containsExactly(PARTNER.toString())
        assertThat(sent().single().text).contains("Аня", "8 450", "Магнум", "1 900")
    }

    private fun operation(amount: String, description: String?) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = groupId,
        creatorId = TEST_USER_ID,
        type = CategoryType.EXPENSE,
        amount = BigDecimal(amount),
        categoryId = checkNotNull(taxi.id),
        categoryName = taxi.name,
        categoryIcon = taxi.icon,
        operationDate = LocalDate.of(2026, 11, 4),
        description = description,
    )

    private fun sent(): List<SendMessage> =
        mockingDetails(notifier).invocations.filter { it.method.name == "execute" }.map { it.arguments.single() as SendMessage }
}

private class MovableClock(private var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
    fun advanceMinutes(minutes: Long) {
        now = now.plusSeconds(minutes * 60)
    }
}
