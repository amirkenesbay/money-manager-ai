package ai.moneymanager.web.report

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryComparison
import ai.moneymanager.domain.model.report.CategoryMonthData
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.ExpensiveDay
import ai.moneymanager.domain.model.report.MaxExpense
import ai.moneymanager.domain.model.report.MemberTotal
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.service.CATEGORY_REPORT_DEFAULT_MONTHS
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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

private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
private val CURRENT_MONTH: YearMonth = YearMonth.from(TODAY)
private const val REPORT_METHOD_PREFIX = "build"

@WebMvcTest(controllers = [ReportController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, GroupCategoryLookup::class, HistoryAccessPolicy::class, SubscriptionLimitsService::class, ReportResponseFactory::class)
class ReportControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var financeReportService: FinanceReportService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @TestConfiguration
    class FixedClock {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    }

    private val groupId = ObjectId()
    private val foreignGroupId = ObjectId()
    private val group = testGroup(groupId, members = setOf(TEST_USER_ID, TEST_USER_ID + 1))
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val foreignCategory = Category(ObjectId(), "Чужая", null, CategoryType.EXPENSE, foreignGroupId)

    @BeforeEach
    fun givenGroup() {
        givenCurrentUser(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))
        `when`(categoryService.getCategory(groceries.id!!)).thenReturn(groceries)
        `when`(categoryService.getCategory(foreignCategory.id!!)).thenReturn(foreignCategory)
        listOf(1, CATEGORY_REPORT_DEFAULT_MONTHS).forEach { months ->
            `when`(financeReportService.categoryReportStart(months)).thenReturn(CURRENT_MONTH.minusMonths(months - 1L).atDay(1))
        }
    }

    @Test
    fun `analytics returns dated data of the current month by default`() {
        `when`(financeReportService.buildAnalyticsReport(groupId, CURRENT_MONTH.atDay(1), null)).thenReturn(
            AnalyticsReport(
                monthName = "Октябрь", year = 2026, totalExpense = BigDecimal("15000"), totalIncome = BigDecimal("450000"),
                operationCount = 7, daysInMonth = 31, topExpenses = listOf(CategoryTotal("🛒", "Продукты", BigDecimal("12000"))),
                maxExpense = MaxExpense(BigDecimal("8000"), "🛒", "Продукты", 12, "окт"),
                mostExpensiveDay = ExpensiveDay(12, "октября", BigDecimal("9000")), isEmpty = false,
            )
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/reports/analytics").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value("2026-10"))
            .andExpect(jsonPath("$.expense").value("15000"))
            .andExpect(jsonPath("$.daysInMonth").value(31))
            .andExpect(jsonPath("$.topExpenses[0].name").value("Продукты"))
            .andExpect(jsonPath("$.maxExpense.date").value("2026-10-12"))
            .andExpect(jsonPath("$.mostExpensiveDay.total").value("9000"))
    }

    @Test
    fun `analytics of a month beyond free history asks for premium`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/analytics").withTelegramAuth().param("month", "2026-07"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"))

        assertNoReportBuilt()
    }

    @Test
    fun `comparison needs the previous month, so free users are asked for premium`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/comparison").withTelegramAuth())
            .andExpect(status().isUnprocessableContent)

        assertNoReportBuilt()
    }

    @Test
    fun `comparison pairs both months for premium users`() {
        givenCurrentUser(premiumUser())
        `when`(financeReportService.buildComparisonReport(groupId, CURRENT_MONTH.minusMonths(1).atDay(1), null)).thenReturn(
            ComparisonReport(
                previousMonthName = "Сен", currentMonthName = "Окт", year = "2026",
                previousExpenseTotal = BigDecimal("20000"), currentExpenseTotal = BigDecimal("15000"),
                previousIncomeTotal = BigDecimal("400000"), currentIncomeTotal = BigDecimal("450000"),
                categoryComparisons = listOf(CategoryComparison("🛒", "Продукты", BigDecimal("14000"), BigDecimal("12000"))),
                isEmpty = false,
            )
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/reports/comparison").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value("2026-10"))
            .andExpect(jsonPath("$.previousMonth").value("2026-09"))
            .andExpect(jsonPath("$.expense.previous").value("20000"))
            .andExpect(jsonPath("$.expense.current").value("15000"))
            .andExpect(jsonPath("$.categories[0].previous").value("14000"))
    }

    @Test
    fun `members report names every member of the group`() {
        val names = mapOf(TEST_USER_ID to "Амир", TEST_USER_ID + 1 to "@esen")
        `when`(userInfoService.displayNames(group.memberIds)).thenReturn(names)
        `when`(financeReportService.buildMembersReport(groupId, CURRENT_MONTH.atDay(1), names, null)).thenReturn(
            MembersReport(
                monthName = "Октябрь", year = 2026,
                expensesByMember = listOf(MemberTotal("Амир", BigDecimal("10000")), MemberTotal("@esen", BigDecimal("5000"))),
                incomesByMember = listOf(MemberTotal("Амир", BigDecimal("450000"))),
                totalExpense = BigDecimal("15000"), totalIncome = BigDecimal("450000"), isEmpty = false,
            )
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/reports/members").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseByMember[1].name").value("@esen"))
            .andExpect(jsonPath("$.incomeByMember[0].total").value("450000"))
    }

    @Test
    fun `category report of the current month is free`() {
        `when`(financeReportService.buildCategoryReport(groupId, groceries.id!!, "🛒", "Продукты", 1, null)).thenReturn(
            CategoryReport("🛒", "Продукты", 1, listOf(CategoryMonthData("Окт 2026", BigDecimal("12000"), 4, CURRENT_MONTH)), BigDecimal("12000"))
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/reports/category").withTelegramAuth().param("categoryId", groceries.id!!.toHexString()).param("months", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Продукты"))
            .andExpect(jsonPath("$.months[0].month").value("2026-10"))
            .andExpect(jsonPath("$.months[0].count").value(4))
    }

    @Test
    fun `category trend over several months asks free users for premium`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/category").withTelegramAuth().param("categoryId", groceries.id!!.toHexString()))
            .andExpect(status().isUnprocessableContent)

        assertNoReportBuilt()
    }

    @Test
    fun `category of another group is not found`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/category").withTelegramAuth().param("categoryId", foreignCategory.id!!.toHexString()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `rejects invalid month and month count`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/analytics").withTelegramAuth().param("month", "2026-13"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/groups/$groupId/reports/category").withTelegramAuth().param("categoryId", groceries.id!!.toHexString()).param("months", "13"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `hides reports of a group the user is not in`() {
        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/reports/members").withTelegramAuth())
            .andExpect(status().isForbidden)
    }

    private fun premiumUser() = testUser().copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = LocalDateTime.now().plusDays(1))

    private fun givenCurrentUser(user: UserInfo) {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(user)
    }

    private fun assertNoReportBuilt() {
        val built = mockingDetails(financeReportService).invocations.any { it.method.name.startsWith(REPORT_METHOD_PREFIX) }
        check(!built) { "report must not be built" }
    }
}
