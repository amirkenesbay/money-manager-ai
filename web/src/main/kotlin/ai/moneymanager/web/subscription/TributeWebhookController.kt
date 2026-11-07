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

@RestController
class TributeWebhookController(
    @Value("\${tribute.api-key:}") apiKey: String,
    private val userInfoService: UserInfoService,
    private val jsonMapper: JsonMapper,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val signature = apiKey.takeIf { it.isNotBlank() }?.let(::TributeSignature)

    @PostMapping(TRIBUTE_WEBHOOK_PATH)
    fun receive(
        @RequestBody body: ByteArray,
        @RequestHeader(SIGNATURE_HEADER, required = false) signatureHeader: String?,
    ): ResponseEntity<Void> {
        val verifier = signature ?: return outcome(HttpStatus.SERVICE_UNAVAILABLE, "unconfigured").also { log.warn("Tribute webhook received but TRIBUTE_API_KEY is not set") }
        if (!verifier.matches(body, signatureHeader)) return outcome(HttpStatus.UNAUTHORIZED, "rejected").also { log.warn("Tribute webhook with a wrong signature") }
        val event = runCatching { jsonMapper.readValue(body, TributeEvent::class.java) }.getOrElse {
            log.warn("Tribute webhook with an unreadable body: {}", it.message)
            return outcome(HttpStatus.BAD_REQUEST, "unreadable")
        }
        val change = event.change
        val payloadNode = event.payload
        if (change == null || payloadNode == null) {
            log.info("Tribute event {} ignored", event.name)
            return outcome(HttpStatus.OK, "ignored")
        }
        val payload = jsonMapper.treeToValue(payloadNode, TributeSubscriptionPayload::class.java)
        val user = userInfoService.applyPaidSubscription(payload.profile(), payload.subscription(change, ZoneId.systemDefault()))
        log.info("Tribute {} applied: user={}, period={}, expiresAt={}", change, user.telegramUserId, payload.period, user.subscriptionExpiresAt)
        return outcome(HttpStatus.OK, "applied")
    }

    private fun outcome(status: HttpStatus, outcome: String): ResponseEntity<Void> {
        meterRegistry.counter(WEBHOOK_METRIC, OUTCOME_TAG, outcome).increment()
        return ResponseEntity.status(status).build()
    }
}
