package ai.moneymanager.web.category

import ai.moneymanager.domain.model.BudgetLimitResult
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryCreationResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.CategoryUpdateResult
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MAX_CATEGORY_NAME_LENGTH
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.json
import ai.moneymanager.web.mockInvoked
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val FREE_CATEGORIES_PER_TYPE = 10
private const val CREATE_CATEGORY = "createCategory"
private const val DELETE_CATEGORY = "deleteCategory"
private const val FREE_BUDGETS = 3

@WebMvcTest(controllers = [CategoryController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, GroupCategoryLookup::class, SubscriptionLimitsService::class)
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
        `when`(categoryService.getCategory(groceries.id!!)).thenReturn(groceries)
    }

    @Test
    fun `puts a monthly limit on a category`() {
        `when`(categoryService.setMonthlyLimit(groceries.id!!, BigDecimal("100000"), false, FREE_BUDGETS))
            .thenReturn(BudgetLimitResult.Updated(groceries.copy(monthlyLimit = BigDecimal("100000"))))

        mockMvc.perform(put("/api/v1/groups/$groupId/categories/${groceries.id}/limit").withTelegramAuth().json("""{"amount":"100000"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.monthlyLimit").value(100000))
    }

    @Test
    fun `explains the free budget limit`() {
        `when`(categoryService.setMonthlyLimit(groceries.id!!, BigDecimal("5000"), false, FREE_BUDGETS)).thenReturn(BudgetLimitResult.LimitReached(FREE_BUDGETS))

        mockMvc.perform(put("/api/v1/groups/$groupId/categories/${groceries.id}/limit").withTelegramAuth().json("""{"amount":"5000"}"""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("BUDGET_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_BUDGETS))
    }

    @Test
    fun `rejects a zero limit before touching the category`() {
        mockMvc.perform(put("/api/v1/groups/$groupId/categories/${groceries.id}/limit").withTelegramAuth().json("""{"amount":"0"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Лимит должен быть больше нуля."))
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
    fun `saves the dragged order of categories`() {
        `when`(categoryService.reorder(groupId, listOf(checkNotNull(groceries.id)))).thenReturn(true)

        mockMvc.perform(put("/api/v1/groups/$groupId/categories/order").withTelegramAuth().json("""{"ids":["${groceries.id!!.toHexString()}"]}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Продукты"))
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

    @Test
    fun `creates a category with a trimmed name`() {
        val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, groupId)
        `when`(categoryService.createCategory(TEST_USER_ID, "Кафе", "☕", CategoryType.EXPENSE, groupId, FREE_CATEGORIES_PER_TYPE))
            .thenReturn(CategoryCreationResult.Created(cafe))

        mockMvc.perform(post(categoriesPath).withTelegramAuth().json(createBody(name = "  Кафе  ")))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(cafe.id!!.toHexString()))
            .andExpect(jsonPath("$.icon").value("☕"))
    }

    @Test
    fun `rejects an empty or too long name`() {
        mockMvc.perform(post(categoriesPath).withTelegramAuth().json(createBody(name = "   ")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Введите название."))
        mockMvc.perform(post(categoriesPath).withTelegramAuth().json(createBody(name = "а".repeat(MAX_CATEGORY_NAME_LENGTH + 1))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Название слишком длинное: максимум $MAX_CATEGORY_NAME_LENGTH символов."))

        check(!mockInvoked(categoryService, CREATE_CATEGORY)) { "invalid names must not reach the service" }
    }

    @Test
    fun `explains a duplicate name`() {
        `when`(categoryService.createCategory(TEST_USER_ID, "Кафе", "☕", CategoryType.EXPENSE, groupId, FREE_CATEGORIES_PER_TYPE))
            .thenReturn(CategoryCreationResult.Duplicate)

        mockMvc.perform(post(categoriesPath).withTelegramAuth().json(createBody()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CATEGORY_DUPLICATE"))
            .andExpect(jsonPath("$.message").value("Категория «Кафе» уже есть."))
    }

    @Test
    fun `offers premium when the free limit is reached`() {
        `when`(categoryService.createCategory(TEST_USER_ID, "Кафе", "☕", CategoryType.EXPENSE, groupId, FREE_CATEGORIES_PER_TYPE))
            .thenReturn(CategoryCreationResult.LimitReached(FREE_CATEGORIES_PER_TYPE))

        mockMvc.perform(post(categoriesPath).withTelegramAuth().json(createBody()))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("CATEGORY_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_CATEGORIES_PER_TYPE))
    }

    @Test
    fun `renames a category on behalf of the user and keeps its icon`() {
        val renamed = groceries.copy(name = "Еда")
        `when`(categoryService.updateCategory(TEST_USER_ID, groceries.id!!, "Еда", "🛒")).thenReturn(CategoryUpdateResult.Updated(renamed))

        mockMvc.perform(patch(categoryPath(groceries)).withTelegramAuth().json("""{"name":"Еда"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Еда"))
            .andExpect(jsonPath("$.icon").value("🛒"))
    }

    @Test
    fun `changes only the icon and keeps the name`() {
        val recolored = groceries.copy(icon = "🥦")
        `when`(categoryService.updateCategory(TEST_USER_ID, groceries.id!!, "Продукты", "🥦")).thenReturn(CategoryUpdateResult.Updated(recolored))

        mockMvc.perform(patch(categoryPath(groceries)).withTelegramAuth().json("""{"icon":"🥦"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.icon").value("🥦"))
    }

    @Test
    fun `explains a rename into an existing name`() {
        `when`(categoryService.updateCategory(TEST_USER_ID, groceries.id!!, "Кафе", "🛒")).thenReturn(CategoryUpdateResult.Duplicate)

        mockMvc.perform(patch(categoryPath(groceries)).withTelegramAuth().json("""{"name":"Кафе"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CATEGORY_DUPLICATE"))
    }

    @Test
    fun `deletes a category on behalf of the user`() {
        `when`(categoryService.deleteCategory(TEST_USER_ID, groceries.id!!)).thenReturn(true)

        mockMvc.perform(delete(categoryPath(groceries)).withTelegramAuth())
            .andExpect(status().isNoContent)
    }

    @Test
    fun `does not touch a category of another group`() {
        val foreign = Category(ObjectId(), "Чужая", "❓", CategoryType.EXPENSE, foreignGroupId)
        `when`(categoryService.getCategory(foreign.id!!)).thenReturn(foreign)

        mockMvc.perform(delete("$categoriesPath/${foreign.id}").withTelegramAuth())
            .andExpect(status().isNotFound)

        check(!mockInvoked(categoryService, DELETE_CATEGORY)) { "a foreign category must stay" }
    }

    private val categoriesPath get() = "/api/v1/groups/$groupId/categories"

    private fun categoryPath(category: Category) = "$categoriesPath/${category.id}"

    private fun createBody(name: String = "Кафе", icon: String = "☕", type: CategoryType = CategoryType.EXPENSE) =
        """{"name":"$name","icon":"$icon","type":"$type"}"""
}
