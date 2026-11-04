package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.ExportFormat
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.export.ExportService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.web.json
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [ExportController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, HistoryAccessPolicy::class, SubscriptionLimitsService::class, UserCalendar::class)
class ExportControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var limits: SubscriptionLimitsService

    @Autowired
    private lateinit var userCalendar: UserCalendar

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var exportService: ExportService

    private val groupId = ObjectId()
    private val group = testGroup(groupId)

    @BeforeEach
    fun givenMember() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
    }

    @Test
    fun `sends the free history by default and tells how many operations went`() {
        val user = testUser()
        `when`(exportService.export(TEST_USER_ID, null, group, limits.earliestHistoryDate(false), userCalendar.today(user), ExportFormat.XLSX)).thenReturn(42)

        mockMvc.perform(post("/api/v1/groups/$groupId/export").withTelegramAuth().json("""{"format":"XLSX"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(42))
    }

    @Test
    fun `asks for Premium beyond the free history`() {
        val tooEarly = checkNotNull(limits.earliestHistoryDate(false)).minusDays(1)

        mockMvc.perform(post("/api/v1/groups/$groupId/export").withTelegramAuth().json("""{"format":"CSV","from":"$tooEarly","to":"${LocalDate.now()}"}"""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"))
    }
}
