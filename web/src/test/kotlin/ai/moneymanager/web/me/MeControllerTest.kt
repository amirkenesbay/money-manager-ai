package ai.moneymanager.web.me

import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verify
import ai.moneymanager.metrics.MessageType
import ai.moneymanager.metrics.MessageMetrics
import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.PREMIUM_PRICES
import ai.moneymanager.domain.model.PREMIUM_SUBSCRIBE_LINK
import ai.moneymanager.domain.model.SubscriptionPeriod
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.testing.ENGLISH
import ai.moneymanager.testing.TEST_FIRST_NAME
import ai.moneymanager.testing.TEST_LANGUAGE_CODE
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.testing.testUser
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.ZoneId

private const val ME_PATH = "/api/v1/me"
private const val UNSUPPORTED_LANGUAGE = "de"
private const val DEVICE_TIMEZONE = "Asia/Almaty"
private const val HINTS_PATH = "$ME_PATH/hints"
private const val OPENED_PATH = "$ME_PATH/opened"
private const val DIGESTS_PATH = "$ME_PATH/digests"

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

    @MockitoBean
    private lateinit var messageMetrics: MessageMetrics

    @Test
    fun `counts an app opening that came from a reminder`() {
        givenCurrentUser(testUser())

        mockMvc.perform(post(OPENED_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"source":"spending_reminder"}"""))
            .andExpect(status().isNoContent)

        verify(messageMetrics).opened(MessageType.SPENDING_REMINDER)
    }

    @Test
    fun `ignores an unknown opening source`() {
        givenCurrentUser(testUser())

        mockMvc.perform(post(OPENED_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"source":"anything"}"""))
            .andExpect(status().isNoContent)

        verifyNoInteractions(messageMetrics)
    }

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
            .andExpect(jsonPath("$.subscription.subscribeUrl").value(PREMIUM_SUBSCRIBE_LINK))
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

    @Test
    fun `remembers the time zone of the device`() {
        givenCurrentUser(testUser())
        `when`(userInfoService.updateTimezone(TEST_USER_ID, DEVICE_TIMEZONE)).thenReturn(testUser().copy(timezone = DEVICE_TIMEZONE))

        mockMvc.perform(patch(ME_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"timezone":"$DEVICE_TIMEZONE"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timezone").value(DEVICE_TIMEZONE))
    }

    @Test
    fun `rejects an unknown time zone`() {
        givenCurrentUser(testUser())

        mockMvc.perform(patch(ME_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"timezone":"Mars/Olympus"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Этот часовой пояс не поддерживается."))
    }

    @Test
    fun `describes premium with its limits and price`() {
        givenCurrentUser(testUser())

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(jsonPath("$.subscription.freeLimits.categoriesPerType").value(limits.maxCategoriesPerType(false)))
            .andExpect(jsonPath("$.subscription.paidLimits.categoriesPerType").value(limits.maxCategoriesPerType(true)))
            .andExpect(jsonPath("$.subscription.paidLimits.ownedSharedGroups").value(nullValue()))
            .andExpect(jsonPath("$.subscription.prices[0].period").value(PREMIUM_PRICES[0].period.name))
            .andExpect(jsonPath("$.subscription.prices[0].amount").value(PREMIUM_PRICES[0].amount.toPlainString()))
            .andExpect(jsonPath("$.subscription.prices[0].currency").value(PREMIUM_PRICES[0].currency.name))
            .andExpect(jsonPath("$.subscription.prices[1].period").value(SubscriptionPeriod.YEAR.name))
    }

    @Test
    fun `gives a whitelisted user premium without an end date`() {
        givenCurrentUser(testUser().copy(access = UserAccess.WHITELISTED))

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.subscription.tier").value("PAID"))
            .andExpect(jsonPath("$.subscription.expiresAt").value(nullValue()))
            .andExpectLimits(paid = true)
    }

    @Test
    fun `refuses a blocked user with a readable reason`() {
        givenCurrentUser(testUser().copy(access = UserAccess.BLOCKED))

        mockMvc.perform(get(ME_PATH).withTelegramAuth())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("USER_BLOCKED"))
            .andExpect(jsonPath("$.message").value("Аккаунт заблокирован."))
    }

    @Test
    fun `remembers a shown hint so it does not repeat on another device`() {
        givenCurrentUser(testUser())
        `when`(userInfoService.markHintSeen(TEST_USER_ID, AppHint.WELCOME)).thenReturn(testUser().copy(seenHints = setOf(AppHint.WELCOME)))

        mockMvc.perform(post(HINTS_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"hint":"WELCOME"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seenHints", contains("WELCOME")))
    }

    @Test
    fun `rejects an unknown hint`() {
        givenCurrentUser(testUser())

        mockMvc.perform(post(HINTS_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"hint":"SECRET"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `mutes a digest kind and shows it among the muted ones`() {
        givenCurrentUser(testUser())
        `when`(userInfoService.setDigestMuted(TEST_USER_ID, DigestKind.DAY, true)).thenReturn(testUser().copy(mutedDigests = setOf(DigestKind.DAY)))

        mockMvc.perform(post(DIGESTS_PATH).withTelegramAuth().contentType(MediaType.APPLICATION_JSON).content("""{"kind":"DAY","muted":true}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mutedDigests", contains("DAY")))
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
        .andExpect(jsonPath("$.subscription.limits.budgets").value(limits.maxBudgets(paid)))
}
