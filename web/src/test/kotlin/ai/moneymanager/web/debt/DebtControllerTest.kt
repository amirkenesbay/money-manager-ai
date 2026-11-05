package ai.moneymanager.web.debt

import ai.moneymanager.domain.model.debt.DebtItem
import ai.moneymanager.domain.model.debt.DebtItemKind
import ai.moneymanager.domain.model.debt.GroupDebts
import ai.moneymanager.domain.model.debt.PersonalDebt
import ai.moneymanager.domain.model.debt.Settlement
import ai.moneymanager.domain.model.debt.Transfer
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.debt.DebtService
import ai.moneymanager.service.debt.ReminderResult
import ai.moneymanager.service.loan.LoanService
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.json
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

private const val ANYA = TEST_USER_ID + 1
private const val DANYA = TEST_USER_ID + 2
private val NAMES = mapOf(TEST_USER_ID to "Амир", ANYA to "Аня", DANYA to "Даня")

@WebMvcTest(controllers = [DebtController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, UserCalendar::class)
class DebtControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userCalendar: UserCalendar

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var debtService: DebtService

    @MockitoBean
    private lateinit var loanService: LoanService

    private val groupId = ObjectId()
    private val group = testGroup(groupId, members = setOf(TEST_USER_ID, ANYA, DANYA))

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(userInfoService.displayNames(anySet())).thenAnswer { invocation -> invocation.getArgument<Set<Long>>(0).associateWith { NAMES.getValue(it) } }
    }

    @Test
    fun `shows my debts with names and what they are made of`() {
        val settlement = Settlement(ObjectId(), ANYA, TEST_USER_ID, BigDecimal("5000"), LocalDate.of(2026, 11, 5), ANYA)
        `when`(debtService.debts(group, TEST_USER_ID)).thenReturn(
            GroupDebts(
                balance = BigDecimal("8500"),
                mine = listOf(PersonalDebt(ANYA, BigDecimal("12000"), listOf(DebtItem(DebtItemKind.EXPENSE, BigDecimal("3500"), LocalDate.of(2026, 11, 2), ObjectId(), "🍔", "Кафе")))),
                others = listOf(Transfer(ANYA, DANYA, BigDecimal("2000"))),
                history = listOf(settlement),
            )
        )

        `when`(loanService.loans(groupId, TEST_USER_ID)).thenReturn(listOf(
            LoanEntity(id = ObjectId(), groupId = groupId, creatorId = TEST_USER_ID, direction = LoanDirection.BORROWED, person = "Мама", amount = BigDecimal("3500"), lentOn = LocalDate.of(2026, 11, 1)),
        ))

        mockMvc.perform(get("/api/v1/groups/$groupId/debts").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value("8500"))
            .andExpect(jsonPath("$.owedToMe").value("12000"))
            .andExpect(jsonPath("$.iOwe").value("3500"))
            .andExpect(jsonPath("$.loans[0].person").value("Мама"))
            .andExpect(jsonPath("$.mine[0].name").value("Аня"))
            .andExpect(jsonPath("$.mine[0].items[0].title").value("Кафе"))
            .andExpect(jsonPath("$.others[0].toName").value("Даня"))
            .andExpect(jsonPath("$.history[0].canUndo").value(false))
    }

    @Test
    fun `records that I paid someone back`() {
        val settlement = Settlement(ObjectId(), TEST_USER_ID, DANYA, BigDecimal("3500"), LocalDate.of(2026, 11, 5), TEST_USER_ID)
        `when`(debtService.settle(group, TEST_USER_ID, TEST_USER_ID, DANYA, BigDecimal("3500"), userCalendar.today(testUser())))
            .thenReturn(settlement)

        mockMvc.perform(post("/api/v1/groups/$groupId/debts/settlements").withTelegramAuth().json("""{"fromId":$TEST_USER_ID,"toId":$DANYA,"amount":3500}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.toName").value("Даня"))
            .andExpect(jsonPath("$.canUndo").value(true))
    }

    @Test
    fun `refuses a repayment of a zero amount`() {
        mockMvc.perform(post("/api/v1/groups/$groupId/debts/settlements").withTelegramAuth().json("""{"fromId":$TEST_USER_ID,"toId":$DANYA,"amount":0}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `undoes only my own repayment`() {
        val settlementId = ObjectId()
        `when`(debtService.undo(group, TEST_USER_ID, settlementId)).thenReturn(false)

        mockMvc.perform(delete("/api/v1/groups/$groupId/debts/settlements/$settlementId").withTelegramAuth())
            .andExpect(status().isNotFound)
    }

    @Test
    fun `reminds once a day`() {
        `when`(debtService.remind(group, TEST_USER_ID, ANYA)).thenReturn(ReminderResult.SENT, ReminderResult.TOO_SOON)

        mockMvc.perform(post("/api/v1/groups/$groupId/debts/reminders").withTelegramAuth().json("""{"userId":$ANYA}""")).andExpect(status().isNoContent)
        mockMvc.perform(post("/api/v1/groups/$groupId/debts/reminders").withTelegramAuth().json("""{"userId":$ANYA}"""))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("REMINDER_TOO_SOON"))
    }
}
