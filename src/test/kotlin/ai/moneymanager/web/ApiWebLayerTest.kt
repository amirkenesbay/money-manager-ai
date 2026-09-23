package ai.moneymanager.web

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.TelegramProfile
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.config.WebSecurityConfig
import ai.moneymanager.web.error.ApiErrorFactory
import ai.moneymanager.web.json.ApiJsonConfig
import ai.moneymanager.web.security.TEST_BOT_TOKEN
import ai.moneymanager.web.security.TelegramInitDataValidator
import ai.moneymanager.web.security.TestInitData
import org.bson.types.ObjectId
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

private const val MEMBER_ID = 42L
private const val INTERNAL_DETAIL = "mongo connection string leaked"
private const val ENGLISH = "en"

@WebMvcTest(controllers = [ProbeController::class], properties = ["chat-machinist.bot.token=$TEST_BOT_TOKEN"])
@Import(
    WebSecurityConfig::class,
    ApiJsonConfig::class,
    ApiErrorFactory::class,
    TelegramInitDataValidator::class,
    GroupAccessGuard::class,
    LocalizationService::class,
)
class ApiWebLayerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    private val ownGroupId = ObjectId()
    private val foreignGroupId = ObjectId()
    private val missingGroupId = ObjectId()

    @BeforeEach
    fun givenData() {
        `when`(groupService.getGroup(ownGroupId)).thenReturn(group(ownGroupId, members = setOf(MEMBER_ID)))
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(group(foreignGroupId, members = setOf(MEMBER_ID + 1)))
        `when`(groupService.getGroup(missingGroupId)).thenReturn(null)
        givenUserLanguage(null)
    }

    @Test
    fun `rejects api request without initData before it reaches a controller`() {
        mockMvc.perform(get("/api/probe/groups/$ownGroupId"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `leaves paths outside the api unauthenticated`() {
        mockMvc.perform(get("/probe/public"))
            .andExpect(status().isOk)
    }

    @Test
    fun `serialises amounts, ids and dates as strings`() {
        mockMvc.perform(authorized(get("/api/probe/groups/$ownGroupId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.groupId").value(ownGroupId.toHexString()))
            .andExpect(jsonPath("$.amount").value("12500.00"))
            .andExpect(jsonPath("$.roundAmount").value("1000"))
            .andExpect(jsonPath("$.date").value("2026-07-23"))
    }

    @Test
    fun `reads ids and amounts sent as strings`() {
        val categoryId = ObjectId()

        mockMvc.perform(
            authorized(post("/api/probe/echo"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"${categoryId.toHexString()}","amount":"99.90"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categoryId").value(categoryId.toHexString()))
            .andExpect(jsonPath("$.amount").value("99.90"))
    }

    @Test
    fun `answers malformed id in body with bad request`() {
        mockMvc.perform(
            authorized(post("/api/probe/echo"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"not-an-id","amount":"1"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `answers malformed id in path with bad request`() {
        mockMvc.perform(authorized(get("/api/probe/groups/not-an-id")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `forbids a group the user is not a member of, in the user language`() {
        givenUserLanguage(ENGLISH)

        mockMvc.perform(authorized(get("/api/probe/groups/$foreignGroupId")))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("You are not a member of this group."))
    }

    @Test
    fun `reports missing group as not found`() {
        mockMvc.perform(authorized(get("/api/probe/groups/$missingGroupId")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `hides internal failure details behind a generic error`() {
        mockMvc.perform(authorized(get("/api/probe/failure")))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(content().string(not(containsString(INTERNAL_DETAIL))))
    }

    private fun authorized(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.header(HttpHeaders.AUTHORIZATION, "tma ${TestInitData.valid(userId = MEMBER_ID)}")

    private fun givenUserLanguage(language: String?) {
        val profile = TelegramProfile(
            telegramUserId = MEMBER_ID,
            username = null,
            firstName = "Amir",
            lastName = null,
            languageCode = "ru",
        )
        `when`(userInfoService.getOrCreate(profile)).thenReturn(
            UserInfo(
                id = ObjectId(),
                username = null,
                firstName = "Amir",
                lastName = null,
                telegramUserId = MEMBER_ID,
                languageCode = "ru",
                language = language,
            )
        )
    }

    private fun group(id: ObjectId, members: Set<Long>) = MoneyGroup(
        id = id,
        name = "Семья",
        inviteToken = "abc123xyz",
        ownerId = members.first(),
        memberIds = members,
        type = GroupType.SHARED,
    )
}

data class ProbeGroupView(
    val groupId: ObjectId,
    val amount: BigDecimal,
    val roundAmount: BigDecimal,
    val date: LocalDate,
)

data class ProbeBody(
    val categoryId: ObjectId,
    val amount: BigDecimal,
)

@RestController
class ProbeController(
    private val groupAccessGuard: GroupAccessGuard,
) {
    @GetMapping("/api/probe/groups/{groupId}")
    fun group(user: UserInfo, @PathVariable groupId: ObjectId): ProbeGroupView {
        val group = groupAccessGuard.requireMember(user, groupId)
        return ProbeGroupView(
            groupId = requireNotNull(group.id),
            amount = BigDecimal("12500.00"),
            roundAmount = BigDecimal("1E+3"),
            date = LocalDate.of(2026, 7, 23),
        )
    }

    @PostMapping("/api/probe/echo")
    fun echo(user: UserInfo, @RequestBody body: ProbeBody): ProbeBody = body

    @GetMapping("/api/probe/failure")
    fun failure(user: UserInfo): ProbeBody = throw IllegalStateException(INTERNAL_DETAIL)

    @GetMapping("/probe/public")
    fun public(): String = "ok"
}
