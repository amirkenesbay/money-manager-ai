package ai.moneymanager.web.category

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CategoryController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class)
class CategoryControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    private val groupId = ObjectId()
    private val foreignGroupId = ObjectId()
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val salary = Category(ObjectId(), "Зарплата", null, CategoryType.INCOME, groupId)

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId))
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(groceries, salary))
        `when`(categoryService.getCategoriesByGroupAndType(groupId, CategoryType.INCOME)).thenReturn(listOf(salary))
    }

    @Test
    fun `lists all categories of the group with a fallback icon`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/categories").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(groceries.id!!.toHexString()))
            .andExpect(jsonPath("$[0].icon").value("🛒"))
            .andExpect(jsonPath("$[1].type").value("INCOME"))
            .andExpect(jsonPath("$[1].icon").value(DEFAULT_CATEGORY_ICON))
    }

    @Test
    fun `filters categories by type`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/categories").withTelegramAuth().param("type", "INCOME"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Зарплата"))
    }

    @Test
    fun `hides categories of a group the user is not in`() {
        mockMvc.perform(get("/api/v1/groups/$foreignGroupId/categories").withTelegramAuth())
            .andExpect(status().isForbidden)
    }
}
