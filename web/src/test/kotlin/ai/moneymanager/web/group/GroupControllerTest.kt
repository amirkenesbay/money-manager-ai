package ai.moneymanager.web.group

import ai.moneymanager.domain.model.CurrencyUpdateResult
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.GroupCreationResult
import ai.moneymanager.domain.model.GroupNoticeLevel
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.buildInviteLink
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.json
import ai.moneymanager.web.mockInvoked
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
import java.math.BigDecimal

private const val OTHER_OWNER_ID = TEST_USER_ID + 1
private const val SWITCH_ACTIVE_GROUP = "switchActiveGroup"
private const val DELETE_GROUP = "deleteGroup"
private const val FREE_SHARED_GROUPS = 3
private const val OTHER_OWNER_NAME = "Айгерим"
private const val TEST_USER_NAME = "Amir"

@WebMvcTest(controllers = [GroupController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, SubscriptionLimitsService::class)
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
    private val ownedGroup = testGroup(members = setOf(TEST_USER_ID, OTHER_OWNER_ID)).copy(name = "Поездка")

    @BeforeEach
    fun givenGroups() {
        givenCurrentUser(testUser(activeGroupId = personalGroup.id))
        `when`(groupService.getUserGroups(TEST_USER_ID)).thenReturn(listOf(personalGroup, familyGroup))
        listOf(personalGroup, familyGroup, foreignGroup, ownedGroup).forEach { `when`(groupService.getGroup(it.id!!)).thenReturn(it) }
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

        check(!mockInvoked(groupService, SWITCH_ACTIVE_GROUP)) { "active group must not change" }
    }

    @Test
    fun `creates a shared group that becomes active`() {
        val created = testGroup().copy(name = "Квартира", currency = Currency.EUR)
        `when`(groupService.createGroup(TEST_USER_ID, "Квартира", Currency.EUR, FREE_SHARED_GROUPS)).thenReturn(GroupCreationResult.Created(created))

        mockMvc.perform(post("/api/v1/groups").withTelegramAuth().json("""{"name":" Квартира ","currency":"EUR"}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Квартира"))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.isActive").value(true))
    }

    @Test
    fun `explains a duplicate group name and the free limit`() {
        `when`(groupService.createGroup(TEST_USER_ID, "Семья", Currency.KZT, FREE_SHARED_GROUPS)).thenReturn(GroupCreationResult.Duplicate)
        `when`(groupService.createGroup(TEST_USER_ID, "Работа", Currency.KZT, FREE_SHARED_GROUPS)).thenReturn(GroupCreationResult.LimitReached(FREE_SHARED_GROUPS))

        mockMvc.perform(post("/api/v1/groups").withTelegramAuth().json("""{"name":"Семья","currency":"KZT"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("GROUP_DUPLICATE"))
        mockMvc.perform(post("/api/v1/groups").withTelegramAuth().json("""{"name":"Работа","currency":"KZT"}"""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("GROUP_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_SHARED_GROUPS))
    }

    @Test
    fun `lets the owner rename a group and change its currency`() {
        val renamed = ownedGroup.copy(name = "Отпуск")
        `when`(groupService.updateGroupName(TEST_USER_ID, ownedGroup.id!!, "Отпуск")).thenReturn(renamed)
        `when`(groupService.updateCurrency(TEST_USER_ID, ownedGroup.id!!, Currency.USD)).thenReturn(CurrencyUpdateResult.Updated(renamed.copy(currency = Currency.USD)))

        mockMvc.perform(patch("/api/v1/groups/${ownedGroup.id}").withTelegramAuth().json("""{"name":"Отпуск","currency":"USD"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Отпуск"))
            .andExpect(jsonPath("$.currency").value("USD"))
    }

    @Test
    fun `refuses a currency without an exchange rate`() {
        `when`(groupService.updateCurrency(TEST_USER_ID, ownedGroup.id!!, Currency.TMT)).thenReturn(CurrencyUpdateResult.RateUnavailable)

        mockMvc.perform(patch("/api/v1/groups/${ownedGroup.id}").withTelegramAuth().json("""{"currency":"TMT"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Нет курса для этой валюты. Выбери другую."))
    }

    @Test
    fun `lets the owner choose the day the month starts`() {
        `when`(groupService.updateMonthStartDay(TEST_USER_ID, ownedGroup.id!!, 25)).thenReturn(ownedGroup.copy(monthStartDay = 25))

        mockMvc.perform(patch("/api/v1/groups/${ownedGroup.id}").withTelegramAuth().json("""{"monthStartDay":25}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.monthStartDay").value(25))
    }

    @Test
    fun `rejects a month start day that some months do not have`() {
        mockMvc.perform(patch("/api/v1/groups/${ownedGroup.id}").withTelegramAuth().json("""{"monthStartDay":31}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Начало месяца — число от 1 до 28."))
    }

    @Test
    fun `explains a rename into a name the owner already uses`() {
        mockMvc.perform(patch("/api/v1/groups/${ownedGroup.id}").withTelegramAuth().json("""{"name":"Личное"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("GROUP_DUPLICATE"))
    }

    @Test
    fun `keeps owner actions away from other members`() {
        mockMvc.perform(patch("/api/v1/groups/${familyGroup.id}").withTelegramAuth().json("""{"currency":"KZT"}"""))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Это может сделать только владелец группы."))
        mockMvc.perform(delete("/api/v1/groups/${familyGroup.id}").withTelegramAuth())
            .andExpect(status().isForbidden)

        check(!mockInvoked(groupService, DELETE_GROUP)) { "a member must not delete the group" }
    }

    @Test
    fun `deletes a shared group of the owner but never the personal one`() {
        mockMvc.perform(delete("/api/v1/groups/${ownedGroup.id}").withTelegramAuth())
            .andExpect(status().isNoContent)
        verify(groupService).deleteGroup(TEST_USER_ID, ownedGroup.id!!)

        mockMvc.perform(delete("/api/v1/groups/${personalGroup.id}").withTelegramAuth())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Личную группу удалить нельзя."))
    }

    @Test
    fun `lists members with the owner first`() {
        `when`(userInfoService.displayNames(familyGroup.memberIds))
            .thenReturn(mapOf(OTHER_OWNER_ID to OTHER_OWNER_NAME, TEST_USER_ID to TEST_USER_NAME))

        mockMvc.perform(get("/api/v1/groups/${familyGroup.id}/members").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value(OTHER_OWNER_NAME))
            .andExpect(jsonPath("$[0].isOwner").value(true))
            .andExpect(jsonPath("$[1].userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$[1].isOwner").value(false))
    }

    @Test
    fun `shares an invite link only for shared groups`() {
        mockMvc.perform(get("/api/v1/groups/${familyGroup.id}/invite").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inviteToken").value(familyGroup.inviteToken))
            .andExpect(jsonPath("$.url").value(buildInviteLink(familyGroup.inviteToken)))
        mockMvc.perform(get("/api/v1/groups/${personalGroup.id}/invite").withTelegramAuth())
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `sets a starting balance but refuses a negative one`() {
        `when`(groupService.setInitialBalance(TEST_USER_ID, familyGroup.id!!, BigDecimal("1200.50"))).thenReturn(familyGroup.copy(initialBalance = BigDecimal("1200.50")))

        mockMvc.perform(put("/api/v1/groups/${familyGroup.id}/initial-balance").withTelegramAuth().json("""{"amount":"1200.50"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.initialBalance").value("1200.50"))
        mockMvc.perform(put("/api/v1/groups/${familyGroup.id}/initial-balance").withTelegramAuth().json("""{"amount":"-5"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Начальный баланс не может быть отрицательным."))
    }

    @Test
    fun `lets a member choose which group notices to get`() {
        val muted = testUser().copy(groupNotices = mapOf(familyGroup.id!!.toHexString() to GroupNoticeLevel.IMPORTANT))
        `when`(userInfoService.setGroupNoticeLevel(TEST_USER_ID, familyGroup.id!!, GroupNoticeLevel.IMPORTANT)).thenReturn(muted)

        mockMvc.perform(put("/api/v1/groups/${familyGroup.id}/notices").withTelegramAuth().json("""{"level":"IMPORTANT"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.noticeLevel").value("IMPORTANT"))
    }

    private fun givenCurrentUser(user: UserInfo) {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(user)
    }
}
