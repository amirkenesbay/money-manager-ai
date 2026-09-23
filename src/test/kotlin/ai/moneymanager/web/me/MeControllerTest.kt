package ai.moneymanager.web.me

import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.ENGLISH
import ai.moneymanager.web.security.TEST_FIRST_NAME
import ai.moneymanager.web.security.TEST_LANGUAGE_CODE
import ai.moneymanager.web.security.TEST_USER_ID
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.testUser
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.ZoneId

private const val ME_PATH = "/api/v1/me"
private const val UNSUPPORTED_LANGUAGE = "de"

@WebMvcTest(controllers = [MeController::class])
@ApiWebLayerSetup
@Import(MeResponseFactory::class, SubscriptionLimitsService::class)
class MeControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var limits: SubscriptionLimitsService

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var onboardingService: OnboardingService

    @Test
    fun `returns profile, active group and free limits from the limits service`() {
        val groupId = ObjectId()
        givenCurrentUser(testUser(activeGroupId = groupId))

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.firstName").value(TEST_FIRST_NAME))
            .andExpect(jsonPath("$.language").value(TEST_LANGUAGE_CODE))
            .andExpect(jsonPath("$.activeGroupId").value(groupId.toHexString()))
            .andExpect(jsonPath("$.subscription.tier").value("FREE"))
            .andExpect(jsonPath("$.subscription.expiresAt").value(nullValue()))
            .andExpectLimits(paid = false)
    }

    @Test
    fun `reports active paid subscription with expiry and unlimited quotas as null`() {
        val expiresAt = LocalDateTime.now().plusDays(10).withNano(0)
        givenCurrentUser(testUser().copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = expiresAt))

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.subscription.tier").value("PAID"))
            .andExpect(jsonPath("$.subscription.expiresAt").value(expiresAt.atZone(ZoneId.systemDefault()).toInstant().toString()))
            .andExpectLimits(paid = true)
    }

    @Test
    fun `treats expired paid subscription as free`() {
        val expired = testUser().copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = LocalDateTime.now().minusDays(1))
        givenCurrentUser(expired)

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(jsonPath("$.subscription.tier").value("FREE"))
            .andExpect(jsonPath("$.subscription.expiresAt").value(nullValue()))
            .andExpectLimits(paid = false)
    }

    @Test
    fun `returns no active group for a user who has not finished onboarding in the bot`() {
        givenCurrentUser(testUser(activeGroupId = null))

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activeGroupId").value(nullValue()))
    }

    @Test
    fun `changes language through the same onboarding step as the bot`() {
        givenCurrentUser(testUser())
        `when`(onboardingService.selectLanguage(TEST_USER_ID, ENGLISH)).thenReturn(testUser(language = ENGLISH))

        mockMvc.perform(patchLanguage(ENGLISH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.language").value(ENGLISH))
    }

    @Test
    fun `rejects unsupported language without touching the user`() {
        givenCurrentUser(testUser())

        mockMvc.perform(patchLanguage(UNSUPPORTED_LANGUAGE))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.details.supported", contains("en", "kk", "ru")))

        assertOnboardingUntouched()
    }

    @Test
    fun `rejects language change without a language`() {
        givenCurrentUser(testUser())

        mockMvc.perform(patch(ME_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        assertOnboardingUntouched()
    }

    private fun patchLanguage(language: String) =
        patch(ME_PATH).withTelegramAuth()
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"language":"$language"}""")

    private fun givenCurrentUser(user: UserInfo) {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(user)
    }

    private fun assertOnboardingUntouched() {
        check(mockingDetails(onboardingService).invocations.isEmpty()) { "onboarding must not be called" }
    }

    private fun ResultActions.andExpectLimits(paid: Boolean): ResultActions = this
        .andExpect(jsonPath("$.subscription.limits.aiRequestsPerDay").value(limits.maxAiRequestsPerDay(paid)))
        .andExpect(jsonPath("$.subscription.limits.categoriesPerType").value(limits.maxCategoriesPerType(paid)))
        .andExpect(jsonPath("$.subscription.limits.ownedSharedGroups").value(limits.maxOwnedSharedGroups(paid)))
        .andExpect(jsonPath("$.subscription.limits.activeNotifications").value(limits.maxActiveNotifications(paid)))
        .andExpect(jsonPath("$.subscription.limits.historyDaysBack").value(limits.maxHistoryDaysBack(paid)))
}
