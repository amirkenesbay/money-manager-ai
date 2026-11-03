package ai.moneymanager.web.category

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CategoryRuleController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class)
class CategoryRuleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @MockitoBean
    private lateinit var categoryRuleService: CategoryRuleService

    private val groupId = ObjectId()

    @BeforeEach
    fun givenMember() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId))
    }

    @Test
    fun `lists remembered places with their categories and skips deleted ones`() {
        val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
        val ruleId = ObjectId()
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(groceries))
        `when`(categoryRuleService.rules(groupId)).thenReturn(
            listOf(CategoryRule(ruleId, "магнум", checkNotNull(groceries.id)), CategoryRule(ObjectId(), "кофе", ObjectId()))
        )

        mockMvc.perform(get("/api/v1/groups/$groupId/category-rules").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ruleId.toHexString()))
            .andExpect(jsonPath("$[0].place").value("магнум"))
            .andExpect(jsonPath("$[0].categoryName").value("Продукты"))
            .andExpect(jsonPath("$[0].categoryIcon").value("🛒"))
    }

    @Test
    fun `forgets a remembered place`() {
        val ruleId = ObjectId()
        `when`(categoryRuleService.forget(groupId, ruleId)).thenReturn(true)

        mockMvc.perform(delete("/api/v1/groups/$groupId/category-rules/$ruleId").withTelegramAuth())
            .andExpect(status().isNoContent)
    }

    @Test
    fun `reports a place that is already gone`() {
        mockMvc.perform(delete("/api/v1/groups/$groupId/category-rules/${ObjectId()}").withTelegramAuth())
            .andExpect(status().isNotFound)
    }

    @Test
    fun `hides places of a group the user is not in`() {
        val foreignGroupId = ObjectId()
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))

        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/category-rules").withTelegramAuth())
            .andExpect(status().isForbidden)
    }
}
