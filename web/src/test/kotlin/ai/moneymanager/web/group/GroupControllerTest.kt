package ai.moneymanager.web.group

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.UserInfo
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
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

private const val OTHER_OWNER_ID = TEST_USER_ID + 1
private const val SWITCH_ACTIVE_GROUP = "switchActiveGroup"

@WebMvcTest(controllers = [GroupController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class)
class GroupControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    private val personalGroup = testGroup(type = GroupType.PERSONAL).copy(name = "Личное", initialBalance = BigDecimal("50000.00"))
    private val familyGroup = testGroup(members = setOf(OTHER_OWNER_ID, TEST_USER_ID)).copy(currency = Currency.USD)
    private val foreignGroup = testGroup(members = setOf(OTHER_OWNER_ID))

    @BeforeEach
    fun givenGroups() {
        givenCurrentUser(testUser(activeGroupId = personalGroup.id))
        `when`(groupService.getUserGroups(TEST_USER_ID)).thenReturn(listOf(personalGroup, familyGroup))
        listOf(personalGroup, familyGroup, foreignGroup).forEach { `when`(groupService.getGroup(it.id!!)).thenReturn(it) }
    }

    @Test
    fun `lists the user groups with ownership, active flag and currency`() {
        mockMvc.perform(get("/api/v1/groups").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(personalGroup.id!!.toHexString()))
            .andExpect(jsonPath("$[0].name").value("Личное"))
            .andExpect(jsonPath("$[0].type").value("PERSONAL"))
            .andExpect(jsonPath("$[0].isOwner").value(true))
            .andExpect(jsonPath("$[0].isActive").value(true))
            .andExpect(jsonPath("$[0].initialBalance").value("50000.00"))
            .andExpect(jsonPath("$[1].isOwner").value(false))
            .andExpect(jsonPath("$[1].isActive").value(false))
            .andExpect(jsonPath("$[1].currency").value("USD"))
            .andExpect(jsonPath("$[1].memberCount").value(2))
    }

    @Test
    fun `makes a group the user belongs to active`() {
        mockMvc.perform(post("/api/v1/groups/${familyGroup.id}/activate").withTelegramAuth())
            .andExpect(status().isNoContent)

        verify(groupService).switchActiveGroup(TEST_USER_ID, familyGroup.id!!)
    }

    @Test
    fun `refuses to activate a group the user is not in`() {
        mockMvc.perform(post("/api/v1/groups/${foreignGroup.id}/activate").withTelegramAuth())
            .andExpect(status().isForbidden)

        val switched = mockingDetails(groupService).invocations.any { it.method.name == SWITCH_ACTIVE_GROUP }
        check(!switched) { "active group must not change" }
    }

    private fun givenCurrentUser(user: UserInfo) {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(user)
    }
}
