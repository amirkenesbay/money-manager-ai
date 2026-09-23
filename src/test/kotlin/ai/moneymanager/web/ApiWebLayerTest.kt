package ai.moneymanager.web

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.security.TEST_USER_ID
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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
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

private const val INTERNAL_DETAIL = "mongo connection string leaked"

@WebMvcTest(controllers = [ProbeController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class)
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
        `when`(groupService.getGroup(ownGroupId)).thenReturn(testGroup(ownGroupId, members = setOf(TEST_USER_ID)))
        `when`(groupService.getGroup(foreignGroupId)).thenReturn(testGroup(foreignGroupId, members = setOf(TEST_USER_ID + 1)))
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
        mockMvc.perform(get("/api/probe/groups/$ownGroupId").withTelegramAuth())
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
            post("/api/probe/echo").withTelegramAuth()
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
            post("/api/probe/echo").withTelegramAuth()
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"not-an-id","amount":"1"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `answers malformed id in path with bad request`() {
        mockMvc.perform(get("/api/probe/groups/not-an-id").withTelegramAuth())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `forbids a group the user is not a member of, in the user language`() {
        givenUserLanguage(ENGLISH)

        mockMvc.perform(get("/api/probe/groups/$foreignGroupId").withTelegramAuth())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("You are not a member of this group."))
    }

    @Test
    fun `reports missing group as not found`() {
        mockMvc.perform(get("/api/probe/groups/$missingGroupId").withTelegramAuth())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `hides internal failure details behind a generic error`() {
        mockMvc.perform(get("/api/probe/failure").withTelegramAuth())
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(content().string(not(containsString(INTERNAL_DETAIL))))
    }

    private fun givenUserLanguage(language: String?) {
        `when`(userInfoService.getOrCreate(TestInitData.profile(TEST_USER_ID))).thenReturn(testUser(language = language))
    }

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
