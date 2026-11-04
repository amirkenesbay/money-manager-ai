package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.RecurringDraft
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.domain.model.RecurringSaveResult
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.json
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private val RECURRING_TODAY: LocalDate = LocalDate.of(2026, 11, 15)
private const val FREE_RULES = 3

@WebMvcTest(controllers = [RecurringController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, GroupCategoryLookup::class, OperationDraftFactory::class, UserCalendar::class, SubscriptionLimitsService::class)
class RecurringControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @MockitoBean
    private lateinit var recurringPaymentService: RecurringPaymentService

    @TestConfiguration
    class FixedClock {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(RECURRING_TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }

    private val groupId = ObjectId()
    private val subscriptions = Category(ObjectId(), "Подписки", "📺", CategoryType.EXPENSE, groupId)
    private val draft = RecurringDraft(CategoryType.EXPENSE, BigDecimal("3500"), subscriptions, "Netflix", Recurrence(RecurrenceUnit.MONTH), RECURRING_TODAY, null, RecurringMode.AUTO)

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId))
        `when`(categoryService.getCategory(subscriptions.id!!)).thenReturn(subscriptions)
    }

    @Test
    fun `creates a recurring payment after the first one is recorded`() {
        `when`(recurringPaymentService.create(TEST_USER_ID, groupId, draft, true, FREE_RULES, RECURRING_TODAY))
            .thenReturn(RecurringSaveResult.Saved(payment(RecurringMode.AUTO)))

        mockMvc.perform(post("/api/v1/groups/$groupId/recurring").withTelegramAuth().json(body(firstRecorded = true)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.nextDate").value("2026-12-15"))
            .andExpect(jsonPath("$.monthlyAmount").value("3500.00"))
    }

    @Test
    fun `explains the free limit`() {
        `when`(recurringPaymentService.create(TEST_USER_ID, groupId, draft, false, FREE_RULES, RECURRING_TODAY))
            .thenReturn(RecurringSaveResult.LimitReached(FREE_RULES))

        mockMvc.perform(post("/api/v1/groups/$groupId/recurring").withTelegramAuth().json(body()))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("RECURRING_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_RULES))
    }

    @Test
    fun `refuses an end before the start`() {
        mockMvc.perform(post("/api/v1/groups/$groupId/recurring").withTelegramAuth().json(body(endDate = "2026-11-01")))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `sums what active payments cost a month`() {
        val yearly = payment(RecurringMode.REMIND).copy(id = ObjectId(), unit = RecurrenceUnit.YEAR, amount = BigDecimal("12000"))
        val paused = payment(RecurringMode.AUTO).copy(id = ObjectId(), active = false)
        `when`(recurringPaymentService.payments(groupId)).thenReturn(listOf(payment(RecurringMode.AUTO), yearly, paused))

        mockMvc.perform(get("/api/v1/groups/$groupId/recurring").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.monthlyExpense").value("4500.00"))
    }

    private fun body(firstRecorded: Boolean = false, endDate: String? = null): String {
        val end = endDate?.let { "\"$it\"" } ?: "null"
        return """{"type":"EXPENSE","amount":3500,"categoryId":"${subscriptions.id!!.toHexString()}","description":" Netflix ","unit":"MONTH","interval":1,"startDate":"$RECURRING_TODAY","endDate":$end,"mode":"AUTO","firstRecorded":$firstRecorded}"""
    }

    private fun payment(mode: RecurringMode) = RecurringPaymentEntity(
        id = ObjectId(), groupId = groupId, creatorId = TEST_USER_ID, type = CategoryType.EXPENSE, amount = BigDecimal("3500"),
        categoryId = subscriptions.id!!, categoryName = subscriptions.name, categoryIcon = subscriptions.icon, description = "Netflix",
        unit = RecurrenceUnit.MONTH, interval = 1, startDate = RECURRING_TODAY, endDate = null, nextDate = LocalDate.of(2026, 12, 15), mode = mode,
    )
}
