package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.OperationTotals
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.HistoryOperation
import ai.moneymanager.domain.model.report.HistoryReport
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

private const val DEFAULT_PAGE_SIZE = 20
private const val CREATOR_NAME = "Amir"
private const val FIND_OPERATIONS = "findOperations"
private const val SALARY_DAY = 25
private val SUMMARY_TODAY: LocalDate = LocalDate.of(2026, 10, 15)
private val SUMMARY_MONTH: YearMonth = YearMonth.from(SUMMARY_TODAY)

@WebMvcTest(controllers = [GroupFinanceController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, HistoryAccessPolicy::class, FinanceResponseFactory::class, SubscriptionLimitsService::class)
class GroupFinanceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var limits: SubscriptionLimitsService

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var financeOperationService: FinanceOperationService

    @MockitoBean
    private lateinit var financeHistoryService: FinanceHistoryService

    @TestConfiguration
    class FixedClock {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(SUMMARY_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    }

    private val groupId = ObjectId()
    private val foreignGroupId = ObjectId()
    private val initialBalance = BigDecimal("50000.00")

    @BeforeEach
    fun givenGroups() {
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId).copy(initialBalance = initialBalance))
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))
        `when`(userInfoService.displayNames(setOf(TEST_USER_ID))).thenReturn(mapOf(TEST_USER_ID to CREATOR_NAME))
        givenCurrentUser(testUser())
    }

    @Test
    fun `returns the balance breakdown in the group currency`() {
        `when`(financeOperationService.calculateBalance(groupId, initialBalance)).thenReturn(
            BalanceBreakdown(initialBalance, BigDecimal("300000.00"), BigDecimal("180000.00"), BigDecimal("170000.00"))
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/balance").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currency").value("KZT"))
            .andExpect(jsonPath("$.initial").value("50000.00"))
            .andExpect(jsonPath("$.income").value("300000.00"))
            .andExpect(jsonPath("$.expense").value("180000.00"))
            .andExpect(jsonPath("$.total").value("170000.00"))
    }

    @Test
    fun `hides the balance of a group the user is not in`() {
        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/balance").withTelegramAuth())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `summarises the current month of the group`() {
        val report = HistoryReport(
            startDate = SUMMARY_MONTH.atDay(1),
            endDate = SUMMARY_MONTH.atEndOfMonth(),
            operations = listOf(
                historyOperation(CategoryType.EXPENSE, "3000", SUMMARY_MONTH.atDay(5)),
                historyOperation(CategoryType.EXPENSE, "12000.50", SUMMARY_MONTH.atDay(2)),
                historyOperation(CategoryType.INCOME, "450000", SUMMARY_MONTH.atDay(2)),
                historyOperation(CategoryType.EXPENSE, "500", SUMMARY_MONTH.atDay(5)),
            ),
            incomeByCategory = emptyList(),
            expenseByCategory = listOf(CategoryTotal("🛒", "Продукты", BigDecimal("12000.50")), CategoryTotal("🚕", "Такси", BigDecimal("3000"))),
            totalIncome = BigDecimal("450000"),
            totalExpense = BigDecimal("15000.50"),
            isEmpty = false,
        )
        `when`(financeHistoryService.buildHistoryReport(groupId, SUMMARY_MONTH.atDay(1), SUMMARY_MONTH.atEndOfMonth())).thenReturn(report)

        mockMvc.perform(get("/api/v1/groups/$groupId/summary").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(SUMMARY_MONTH.toString()))
            .andExpect(jsonPath("$.currency").value("KZT"))
            .andExpect(jsonPath("$.income").value("450000"))
            .andExpect(jsonPath("$.expense").value("15000.50"))
            .andExpect(jsonPath("$.net").value("434999.50"))
            .andExpect(jsonPath("$.expenseByCategory[0].name").value("Продукты"))
            .andExpect(jsonPath("$.expenseByCategory[0].total").value("12000.50"))
            .andExpect(jsonPath("$.expenseByCategory[1].icon").value("🚕"))
            .andExpect(jsonPath("$.dailyExpense.length()").value(2))
            .andExpect(jsonPath("$.dailyExpense[0].date").value(SUMMARY_MONTH.atDay(2).toString()))
            .andExpect(jsonPath("$.dailyExpense[0].total").value("12000.50"))
            .andExpect(jsonPath("$.dailyExpense[1].date").value(SUMMARY_MONTH.atDay(5).toString()))
            .andExpect(jsonPath("$.dailyExpense[1].total").value("3500"))
    }

    private fun historyOperation(type: CategoryType, amount: String, date: LocalDate) =
        HistoryOperation(null, type, BigDecimal(amount), "Продукты", "🛒", date, null, TEST_USER_ID)

    @Test
    fun `counts the month from the group start day`() {
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId).copy(monthStartDay = SALARY_DAY))
        val from = LocalDate.of(2026, 9, SALARY_DAY)
        val to = LocalDate.of(2026, 10, SALARY_DAY - 1)
        val report = HistoryReport(from, to, emptyList(), emptyList(), emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, isEmpty = true)
        `when`(financeHistoryService.buildHistoryReport(groupId, from, to)).thenReturn(report)

        mockMvc.perform(get("/api/v1/groups/$groupId/summary").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value("2026-09"))
            .andExpect(jsonPath("$.from").value(from.toString()))
            .andExpect(jsonPath("$.to").value(to.toString()))
    }

    @Test
    fun `hides the month summary of a group the user is not in`() {
        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/summary").withTelegramAuth())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `lists free user operations from the start of the allowed history by default`() {
        val operation = operation(icon = null)
        givenOperations(OperationFilter(groupId, from = limits.earliestHistoryDate(false)), listOf(operation), total = 43)

        mockMvc.perform(get("/api/v1/groups/$groupId/operations").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(checkNotNull(operation.id).toHexString()))
            .andExpect(jsonPath("$.items[0].type").value("EXPENSE"))
            .andExpect(jsonPath("$.items[0].amount").value("3500.00"))
            .andExpect(jsonPath("$.items[0].categoryIcon").value(DEFAULT_CATEGORY_ICON))
            .andExpect(jsonPath("$.items[0].operationDate").value(operation.operationDate.toString()))
            .andExpect(jsonPath("$.items[0].creatorName").value(CREATOR_NAME))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(DEFAULT_PAGE_SIZE))
            .andExpect(jsonPath("$.totalItems").value(43))
    }

    @Test
    fun `searches by text and author and sums what it found`() {
        val filter = OperationFilter(groupId, from = limits.earliestHistoryDate(false), creatorId = TEST_USER_ID, query = "#отпуск")
        givenOperations(filter, emptyList())
        `when`(financeOperationService.totals(filter)).thenReturn(OperationTotals(BigDecimal.ZERO, BigDecimal("12500"), 3))

        mockMvc.perform(
            get("/api/v1/groups/$groupId/operations").withTelegramAuth()
                .param("q", "#отпуск")
                .param("creatorId", TEST_USER_ID.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totals.expense").value("12500"))
            .andExpect(jsonPath("$.totals.count").value(3))
    }

    @Test
    fun `passes type, category, period and page to the query`() {
        val categoryId = ObjectId()
        val from = LocalDate.now().minusDays(7)
        val to = LocalDate.now()
        givenOperations(OperationFilter(groupId, from, to, CategoryType.INCOME, categoryId), emptyList(), page = 2, size = 5)

        mockMvc.perform(
            get("/api/v1/groups/$groupId/operations").withTelegramAuth()
                .param("from", from.toString())
                .param("to", to.toString())
                .param("type", "INCOME")
                .param("categoryId", categoryId.toHexString())
                .param("page", "2")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5))
    }

    @Test
    fun `asks free user for premium when the period starts beyond the history limit`() {
        val tooEarly = checkNotNull(limits.earliestHistoryDate(false)).minusDays(1)

        mockMvc.perform(get("/api/v1/groups/$groupId/operations").withTelegramAuth().param("from", tooEarly.toString()))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"))
            .andExpect(jsonPath("$.details.historyDaysBack").value(limits.maxHistoryDaysBack(false)))

        assertNoOperationsQueried()
    }

    @Test
    fun `lets premium user read the whole history`() {
        givenCurrentUser(testUser().copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = LocalDateTime.now().plusDays(1)))
        val longAgo = LocalDate.of(2020, 1, 1)
        givenOperations(OperationFilter(groupId, from = longAgo), emptyList())

        mockMvc.perform(get("/api/v1/groups/$groupId/operations").withTelegramAuth().param("from", longAgo.toString()))
            .andExpect(status().isOk)
    }

    @Test
    fun `does not reveal operations of a group the user is not in`() {
        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/operations").withTelegramAuth())
            .andExpect(status().isForbidden)

        assertNoOperationsQueried()
    }

    @Test
    fun `rejects a period that ends before it starts`() {
        mockMvc.perform(
            get("/api/v1/groups/$groupId/operations").withTelegramAuth()
                .param("from", LocalDate.now().toString())
                .param("to", LocalDate.now().minusDays(1).toString())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects page sizes outside the allowed range`() {
        listOf("0", "101").forEach { size ->
            mockMvc.perform(get("/api/v1/groups/$groupId/operations").withTelegramAuth().param("size", size))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `rejects an unknown operation type`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/operations").withTelegramAuth().param("type", "TRANSFER"))
            .andExpect(status().isBadRequest)
    }

    private fun givenCurrentUser(user: UserInfo) {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(user)
    }

    private fun givenOperations(
        filter: OperationFilter,
        operations: List<FinanceOperationEntity>,
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        total: Long = operations.size.toLong(),
    ) {
        `when`(financeOperationService.findOperations(filter, page, size))
            .thenReturn(PageImpl(operations, PageRequest.of(page, size), total))
    }

    private fun assertNoOperationsQueried() {
        val queried = mockingDetails(financeOperationService).invocations.any { it.method.name == FIND_OPERATIONS }
        check(!queried) { "operations must not be queried" }
    }

    private fun operation(icon: String?) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = groupId,
        creatorId = TEST_USER_ID,
        type = CategoryType.EXPENSE,
        amount = BigDecimal("3500.00"),
        categoryId = ObjectId(),
        categoryName = "Продукты",
        categoryIcon = icon,
        operationDate = LocalDate.now().minusDays(1),
        description = "Магнум",
    )
}
