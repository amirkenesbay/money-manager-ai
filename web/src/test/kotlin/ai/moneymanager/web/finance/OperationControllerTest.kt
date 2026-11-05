package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.json
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
private const val CREATOR_NAME = "Amir"

private const val PARTNER_ID = TEST_USER_ID + 1
private const val STRANGER_ID = TEST_USER_ID + 99

@WebMvcTest(controllers = [OperationController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, GroupCategoryLookup::class, OperationDraftFactory::class, FinanceResponseFactory::class, UserCalendar::class)
class OperationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @MockitoBean
    private lateinit var financeOperationService: FinanceOperationService

    @TestConfiguration
    class FixedClock {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }

    private val groupId = ObjectId()
    private val foreignGroupId = ObjectId()
    private val operationId = ObjectId()
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val foreignCategory = Category(ObjectId(), "Чужая", "❓", CategoryType.EXPENSE, foreignGroupId)

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(userInfoService.displayNames(setOf(TEST_USER_ID))).thenReturn(mapOf(TEST_USER_ID to CREATOR_NAME))
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId))
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))
        `when`(categoryService.getCategory(groceries.id!!)).thenReturn(groceries)
        `when`(categoryService.getCategory(foreignCategory.id!!)).thenReturn(foreignCategory)
    }

    @Test
    fun `creates an operation on behalf of the user`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("5000"), groceries, TODAY, "Magnum")
        `when`(financeOperationService.create(groupId, TEST_USER_ID, draft)).thenReturn(saved(draft))

        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(description = "  Magnum  ")))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(operationId.toHexString()))
            .andExpect(jsonPath("$.amount").value("5000"))
            .andExpect(jsonPath("$.categoryName").value("Продукты"))
            .andExpect(jsonPath("$.description").value("Magnum"))
            .andExpect(jsonPath("$.creatorName").value(CREATOR_NAME))
    }

    @Test
    fun `passes on that the user corrected the voice category so it is remembered`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("5000"), groceries, TODAY, "Magnum")
        `when`(financeOperationService.create(groupId, TEST_USER_ID, draft, true)).thenReturn(saved(draft))

        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(description = "Magnum", categoryCorrected = true)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(operationId.toHexString()))
    }

    @Test
    fun `stores a blank comment as no comment`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("5000"), groceries, TODAY, null)
        `when`(financeOperationService.create(groupId, TEST_USER_ID, draft)).thenReturn(saved(draft))

        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(description = "   ")))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.description").doesNotExist())
    }

    @Test
    fun `passes on who paid and for whom`() {
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId, members = setOf(TEST_USER_ID, PARTNER_ID)))
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("5000"), groceries, TODAY, null, ExpenseShare(PARTNER_ID, setOf(TEST_USER_ID, PARTNER_ID)))
        `when`(financeOperationService.create(groupId, TEST_USER_ID, draft)).thenReturn(saved(draft).copy(payerId = PARTNER_ID, participantIds = setOf(TEST_USER_ID, PARTNER_ID)))

        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(share = shareJson(PARTNER_ID, TEST_USER_ID, PARTNER_ID))))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.payerId").value(PARTNER_ID))
            .andExpect(jsonPath("$.participantIds.length()").value(2))
    }

    @Test
    fun `rejects a share with someone outside the group`() {
        expectRejected(body(share = shareJson(TEST_USER_ID, STRANGER_ID)), "Выбери участников группы.")
    }

    @Test
    fun `rejects a zero amount`() {
        expectRejected(body(amount = "0"), "Сумма должна быть больше нуля.")
    }

    @Test
    fun `rejects a date in the future`() {
        expectRejected(body(date = TODAY.plusDays(2)), "Операцию нельзя записать будущей датой.")
    }

    @Test
    fun `accepts today of a user ahead of the server clock`() {
        val tomorrowInTheEarliestZone = TODAY.plusDays(1)
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("5000"), groceries, tomorrowInTheEarliestZone, null)
        `when`(financeOperationService.create(groupId, TEST_USER_ID, draft)).thenReturn(saved(draft))

        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(date = tomorrowInTheEarliestZone)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `rejects a comment over the limit`() {
        expectRejected(body(description = "a".repeat(MAX_DESCRIPTION_LENGTH + 1)), "Комментарий слишком длинный: максимум $MAX_DESCRIPTION_LENGTH символов.")
    }

    @Test
    fun `rejects a category of another type`() {
        expectRejected(body(type = CategoryType.INCOME), "Эта категория не подходит для выбранного типа операции.")
    }

    @Test
    fun `does not accept a category of another group`() {
        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(body(categoryId = foreignCategory.id!!)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `replaces every field of an operation`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("7000"), groceries, TODAY.minusDays(1), null)
        `when`(financeOperationService.replace(TEST_USER_ID, groupId, operationId, draft)).thenReturn(saved(draft))

        mockMvc.perform(put("/api/v1/groups/$groupId/operations/$operationId").withTelegramAuth().json(body(amount = "7000", date = TODAY.minusDays(1))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value("7000"))
            .andExpect(jsonPath("$.operationDate").value(TODAY.minusDays(1).toString()))
    }

    @Test
    fun `reports an operation that no longer exists`() {
        mockMvc.perform(put("/api/v1/groups/$groupId/operations/$operationId").withTelegramAuth().json(body()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Операция не найдена. Возможно, её уже удалили."))
    }

    @Test
    fun `deletes an operation`() {
        `when`(financeOperationService.deleteOperation(TEST_USER_ID, groupId, operationId)).thenReturn(true)

        mockMvc.perform(delete("/api/v1/groups/$groupId/operations/$operationId").withTelegramAuth())
            .andExpect(status().isNoContent)
    }

    @Test
    fun `does not touch operations of a group the user is not in`() {
        mockMvc.perform(put("/api/v1/groups/$foreignGroupId/operations/$operationId").withTelegramAuth().json(body(categoryId = foreignCategory.id!!)))
            .andExpect(status().isForbidden)
        mockMvc.perform(delete("/api/v1/groups/$foreignGroupId/operations/$operationId").withTelegramAuth())
            .andExpect(status().isForbidden)

        verifyNoInteractions(financeOperationService)
    }

    private fun expectRejected(requestBody: String, message: String) {
        mockMvc.perform(post("/api/v1/groups/$groupId/operations").withTelegramAuth().json(requestBody))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(message))
        verifyNoInteractions(financeOperationService)
    }

    private fun body(
        type: CategoryType = CategoryType.EXPENSE,
        amount: String = "5000",
        categoryId: ObjectId = groceries.id!!,
        date: LocalDate = TODAY,
        description: String? = null,
        categoryCorrected: Boolean = false,
        share: String = "",
    ): String {
        val descriptionJson = description?.let { "\"$it\"" } ?: "null"
        val correctedJson = if (categoryCorrected) ",\"categoryCorrected\":true" else ""
        return """{"type":"$type","amount":$amount,"categoryId":"${categoryId.toHexString()}","operationDate":"$date","description":$descriptionJson$correctedJson$share}"""
    }

    private fun shareJson(payerId: Long, vararg participantIds: Long) =
        ",\"payerId\":$payerId,\"participantIds\":[${participantIds.joinToString(",")}]"

    private fun saved(draft: OperationDraft) = FinanceOperationEntity(
        id = operationId,
        groupId = groupId,
        creatorId = TEST_USER_ID,
        type = draft.type,
        amount = draft.amount,
        categoryId = draft.category.id!!,
        categoryName = draft.category.name,
        categoryIcon = draft.category.icon,
        operationDate = draft.operationDate,
        description = draft.description,
    )
}
