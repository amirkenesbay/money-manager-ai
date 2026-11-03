package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.monthPeriod
import ai.moneymanager.service.BudgetAlert
import ai.moneymanager.service.BudgetAlertService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
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
import java.time.YearMonth

private const val BOT_TOKEN = "123456:TEST"
private const val SECOND_MEMBER = TEST_USER_ID + 1

class BudgetNoticeTest {
    private val groupId = ObjectId()
    private val group = testGroup(groupId, members = setOf(TEST_USER_ID, SECOND_MEMBER))
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId, BigDecimal("20000"))
    private val groupService = mock(GroupService::class.java)
    private val budgetAlertService = mock(BudgetAlertService::class.java)
    private val notifier = spy(
        GroupActivityNotifier(
            groupService, budgetAlertService, mock(UserInfoService::class.java), testLocalizationService(),
            MiniAppGateway(MiniAppProperties(), mock(UserInfoService::class.java), testLocalizationService(), BOT_TOKEN), BOT_TOKEN,
        )
    )

    @Test
    fun `warns every member of the group that a budget is almost spent`() {
        val budget = Budget(taxi, BigDecimal("20000"), BigDecimal("17000"), monthPeriod(YearMonth.of(2026, 10), 1))
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(budgetAlertService.claimNewAlerts(group)).thenReturn(listOf(BudgetAlert(budget, BudgetThreshold.NEAR)))
        doReturn(Message()).`when`(notifier).execute(any(SendMessage::class.java))

        notifier.onOperationsChanged(OperationsChanged(groupId))

        val sent = mockingDetails(notifier).invocations.filter { it.method.name == "execute" }.map { it.arguments.single() as SendMessage }
        assertThat(sent.map { it.chatId }).containsExactlyInAnyOrder(TEST_USER_ID.toString(), SECOND_MEMBER.toString())
        assertThat(sent.first().text).contains("🚕 Такси", "85%", "17 000", "20 000", "31.10", "3 000")
    }
}
