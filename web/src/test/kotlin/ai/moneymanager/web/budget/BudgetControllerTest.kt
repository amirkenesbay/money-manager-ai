package ai.moneymanager.web.budget

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.monthPeriod
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.YearMonth

@WebMvcTest(controllers = [BudgetController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class)
class BudgetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var budgetService: BudgetService

    @Test
    fun `lists budgets with the most spent first`() {
        val groupId = ObjectId()
        val group = testGroup(groupId)
        val period = monthPeriod(YearMonth.of(2026, 10), 1)
        val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId, BigDecimal("20000"))
        val food = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId, BigDecimal("100000"))
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(budgetService.budgets(group)).thenReturn(
            listOf(Budget(food, BigDecimal("100000"), BigDecimal("62000"), period), Budget(taxi, BigDecimal("20000"), BigDecimal("17000"), period))
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/budgets").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Такси"))
            .andExpect(jsonPath("$[0].percent").value(85))
            .andExpect(jsonPath("$[0].remaining").value(3000))
            .andExpect(jsonPath("$[1].name").value("Продукты"))
            .andExpect(jsonPath("$[1].from").value("2026-10-01"))
    }

    @Test
    fun `hides budgets of a group the user is not in`() {
        val foreignGroupId = ObjectId()
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))

        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/budgets").withTelegramAuth())
            .andExpect(status().isForbidden)
    }
}
