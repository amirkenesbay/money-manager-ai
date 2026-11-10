package ai.moneymanager.web.subscription

import ai.moneymanager.domain.model.PaidSubscription
import ai.moneymanager.domain.model.SubscriptionChange
import ai.moneymanager.domain.model.SubscriptionPeriod
import ai.moneymanager.domain.model.TelegramProfile
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.testing.anyOf
import ai.moneymanager.web.mockInvoked
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private const val TRIBUTE_KEY = "tribute-test-key"
private const val BUYER_ID = 555_000_111L
private const val EXPIRES_AT = "2027-11-07T10:00:00Z"

private fun event(name: String, period: String = "yearly") = """
    {"name":"$name","created_at":"2026-11-07T10:00:00Z","sent_at":"2026-11-07T10:00:01Z","payload":{
      "subscription_name":"Premium","subscription_id":1,"period_id":2,"period":"$period","type":"regular",
      "price":4969,"amount":4500,"currency":"usd","trb_user_id":"T-1","telegram_user_id":$BUYER_ID,
      "telegram_username":"buyer","channel_id":3,"channel_name":"mm","expires_at":"$EXPIRES_AT"}}
""".trimIndent()

@WebMvcTest(controllers = [TributeWebhookController::class])
@ApiWebLayerSetup
@Import(SimpleMeterRegistry::class)
@TestPropertySource(properties = ["tribute.api-key=$TRIBUTE_KEY"])
class TributeWebhookControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    private val signature = TributeSignature(TRIBUTE_KEY)

    @Test
    fun `turns a paid yearly subscription into premium until the date tribute sent`() {
        val applied = mutableListOf<Pair<TelegramProfile, PaidSubscription>>()
        `when`(userInfoService.applyPaidSubscription(anyOf(TelegramProfile::class.java), anyOf(PaidSubscription::class.java), anyOf(LocalDateTime::class.java)))
            .thenAnswer { invocation -> testUser().also { applied += invocation.getArgument<TelegramProfile>(0) to invocation.getArgument<PaidSubscription>(1) } }

        send(event("new_subscription")).andExpect(status().isOk)

        val (profile, subscription) = applied.single()
        assertThat(profile.telegramUserId).isEqualTo(BUYER_ID)
        assertThat(profile.username).isEqualTo("buyer")
        assertThat(subscription.change).isEqualTo(SubscriptionChange.STARTED)
        assertThat(subscription.period).isEqualTo(SubscriptionPeriod.YEAR)
        assertThat(subscription.price).isEqualByComparingTo(BigDecimal("49.69"))
        assertThat(subscription.expiresAt).isEqualTo(LocalDateTime.ofInstant(Instant.parse(EXPIRES_AT), ZoneId.systemDefault()))
    }

    @Autowired
    private lateinit var meterRegistry: SimpleMeterRegistry

    @Test
    fun `counts each webhook by outcome`() {
        val ignored = meterRegistry.counter("moneymanager.tribute.webhooks", "outcome", "ignored")
        val before = ignored.count()

        send("""{"name":"new_donation","payload":{}}""")

        assertThat(ignored.count() - before).isEqualTo(1.0)
    }

    @Test
    fun `refuses a request with a wrong signature`() {
        mockMvc.perform(post(TRIBUTE_WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON).header("trbt-signature", "deadbeef").content(event("new_subscription")))
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(userInfoService)
    }

    @Test
    fun `accepts the dashboard test request that comes without an event name`() {
        send("""{"name":null,"created_at":"2026-11-07T10:00:00Z","payload":null}""").andExpect(status().isOk)

        assertThat(mockInvoked(userInfoService, "applyPaidSubscription")).isFalse()
    }

    @Test
    fun `answers bad request to a body that is not json`() {
        send("not json").andExpect(status().isBadRequest)
    }

    @Test
    fun `ignores events that are not about subscriptions`() {
        send("""{"name":"new_donation","payload":{}}""").andExpect(status().isOk)

        assertThat(mockInvoked(userInfoService, "applyPaidSubscription")).isFalse()
    }

    private fun send(body: String) =
        mockMvc.perform(post(TRIBUTE_WEBHOOK_PATH).contentType(MediaType.APPLICATION_JSON).header("trbt-signature", signature.sign(body.toByteArray())).content(body))
}
