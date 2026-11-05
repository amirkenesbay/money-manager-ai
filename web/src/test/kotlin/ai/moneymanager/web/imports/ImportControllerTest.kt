package ai.moneymanager.web.imports

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportCategory
import ai.moneymanager.domain.model.imports.ImportColumn
import ai.moneymanager.domain.model.imports.ImportPreview
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.SkipReason
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.imports.ImportCommit
import ai.moneymanager.service.imports.ImportService
import ai.moneymanager.service.question.AskService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.ai.AiRequestGuard
import ai.moneymanager.web.json
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

private const val TOKEN = "token-1"
private const val FREE_IMPORTS = 3
private const val CATEGORY_LIMIT = 10

@WebMvcTest(controllers = [ImportController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, AiRequestGuard::class)
class ImportControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var importService: ImportService

    @MockitoBean
    private lateinit var subscriptionLimitsService: SubscriptionLimitsService

    @MockitoBean
    private lateinit var aiRateLimitService: AiRateLimitService

    @MockitoBean
    private lateinit var aiPromptService: AiPromptService

    @MockitoBean
    private lateinit var askService: AskService

    private val groupId = ObjectId()
    private val group = testGroup(groupId)
    private val file = "date;categoryName;outcome\n2026-11-03;Кафе;1500".toByteArray()

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(subscriptionLimitsService.maxImports(false)).thenReturn(FREE_IMPORTS)
        `when`(subscriptionLimitsService.maxCategoriesPerType(false)).thenReturn(CATEGORY_LIMIT)
        `when`(importService.importsUsed(TEST_USER_ID)).thenReturn(1)
    }

    @Test
    fun `sums up an uploaded file and tells how many imports are left`() {
        `when`(importService.read(TEST_USER_ID, group, file)).thenReturn(preview())

        mockMvc.perform(post("/api/v1/groups/$groupId/imports/preview").withTelegramAuth().contentType(MediaType.APPLICATION_OCTET_STREAM).content(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.source").value("ZENMONEY"))
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.needsColumns").value(false))
            .andExpect(jsonPath("$.skipped.TRANSFER").value(2))
            .andExpect(jsonPath("$.importsLeft").value(2))
    }

    @Test
    fun `refuses a file it cannot read`() {
        `when`(importService.read(TEST_USER_ID, group, file)).thenThrow(IllegalArgumentException("broken"))

        mockMvc.perform(post("/api/v1/groups/$groupId/imports/preview").withTelegramAuth().contentType(MediaType.APPLICATION_OCTET_STREAM).content(file))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `stops at the free number of imports`() {
        `when`(importService.commit(TOKEN, TEST_USER_ID, group, emptyMap(), FREE_IMPORTS, CATEGORY_LIMIT)).thenReturn(ImportCommit.LimitReached(FREE_IMPORTS))

        mockMvc.perform(post("/api/v1/groups/$groupId/imports/$TOKEN").withTelegramAuth().json("{}"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("IMPORT_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_IMPORTS))
    }

    private fun preview() = ImportPreview(
        token = TOKEN,
        source = ImportSource.ZENMONEY,
        columns = listOf(ImportColumn("date", listOf("2026-11-03"), ColumnRole.DATE), ImportColumn("outcome", listOf("1500"), ColumnRole.EXPENSE)),
        count = 1,
        from = LocalDate.of(2026, 11, 3),
        to = LocalDate.of(2026, 11, 3),
        skipped = mapOf(SkipReason.TRANSFER to 2),
        categories = listOf(ImportCategory("EXPENSE:кафе", "Кафе", CategoryType.EXPENSE, 1, null)),
    )
}
