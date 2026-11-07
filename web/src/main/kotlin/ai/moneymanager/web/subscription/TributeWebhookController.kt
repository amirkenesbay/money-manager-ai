package ai.moneymanager.web.subscription

import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper
import java.time.ZoneId

const val TRIBUTE_WEBHOOK_PATH = "$API_V1_PREFIX/webhooks/tribute"
private const val SIGNATURE_HEADER = "trbt-signature"
private const val WEBHOOK_METRIC = "moneymanager.tribute.webhooks"
private const val OUTCOME_TAG = "outcome"
private const val OUTCOME_UNCONFIGURED = "unconfigured"
private const val OUTCOME_REJECTED = "rejected"
private const val OUTCOME_UNREADABLE = "unreadable"
private const val OUTCOME_IGNORED = "ignored"
private const val OUTCOME_APPLIED = "applied"
private val OUTCOMES = listOf(OUTCOME_UNCONFIGURED, OUTCOME_REJECTED, OUTCOME_UNREADABLE, OUTCOME_IGNORED, OUTCOME_APPLIED)

@RestController
class TributeWebhookController(
    @Value("\${tribute.api-key:}") apiKey: String,
    private val userInfoService: UserInfoService,
    private val jsonMapper: JsonMapper,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val signature = apiKey.takeIf { it.isNotBlank() }?.let(::TributeSignature)

    init {
        OUTCOMES.forEach { meterRegistry.counter(WEBHOOK_METRIC, OUTCOME_TAG, it) }
    }

    @PostMapping(TRIBUTE_WEBHOOK_PATH)
    fun receive(
        @RequestBody body: ByteArray,
        @RequestHeader(SIGNATURE_HEADER, required = false) signatureHeader: String?,
    ): ResponseEntity<Void> {
        val verifier = signature ?: return outcome(HttpStatus.SERVICE_UNAVAILABLE, OUTCOME_UNCONFIGURED).also { log.warn("Tribute webhook received but TRIBUTE_API_KEY is not set") }
        if (!verifier.matches(body, signatureHeader)) return outcome(HttpStatus.UNAUTHORIZED, OUTCOME_REJECTED).also { log.warn("Tribute webhook with a wrong signature") }
        val event = runCatching { jsonMapper.readValue(body, TributeEvent::class.java) }.getOrElse {
            log.warn("Tribute webhook with an unreadable body: {}", it.message)
            return outcome(HttpStatus.BAD_REQUEST, OUTCOME_UNREADABLE)
        }
        val change = event.change
        val payloadNode = event.payload
        if (change == null || payloadNode == null) {
            log.info("Tribute event {} ignored", event.name)
            return outcome(HttpStatus.OK, OUTCOME_IGNORED)
        }
        val payload = jsonMapper.treeToValue(payloadNode, TributeSubscriptionPayload::class.java)
        val user = userInfoService.applyPaidSubscription(payload.profile(), payload.subscription(change, ZoneId.systemDefault()))
        log.info("Tribute {} applied: user={}, period={}, expiresAt={}", change, user.telegramUserId, payload.period, user.subscriptionExpiresAt)
        return outcome(HttpStatus.OK, OUTCOME_APPLIED)
    }

    private fun outcome(status: HttpStatus, outcome: String): ResponseEntity<Void> {
        meterRegistry.counter(WEBHOOK_METRIC, OUTCOME_TAG, outcome).increment()
        return ResponseEntity.status(status).build()
    }
}
