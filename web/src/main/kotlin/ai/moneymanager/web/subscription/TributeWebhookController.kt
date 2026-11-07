package ai.moneymanager.web.subscription

import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
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

@RestController
class TributeWebhookController(
    @Value("\${tribute.api-key:}") apiKey: String,
    private val userInfoService: UserInfoService,
    private val jsonMapper: JsonMapper,
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val signature = apiKey.takeIf { it.isNotBlank() }?.let(::TributeSignature)

    @PostMapping(TRIBUTE_WEBHOOK_PATH)
    fun receive(
        @RequestBody body: ByteArray,
        @RequestHeader(SIGNATURE_HEADER, required = false) signatureHeader: String?,
    ): ResponseEntity<Void> {
        val verifier = signature ?: return status(HttpStatus.SERVICE_UNAVAILABLE).also { log.warn("Tribute webhook received but TRIBUTE_API_KEY is not set") }
        if (!verifier.matches(body, signatureHeader)) return status(HttpStatus.UNAUTHORIZED).also { log.warn("Tribute webhook with a wrong signature") }
        val event = jsonMapper.readValue(body, TributeEvent::class.java)
        val change = event.change
        val payloadNode = event.payload
        if (change == null || payloadNode == null) {
            log.info("Tribute event {} ignored", event.name)
            return status(HttpStatus.OK)
        }
        val payload = jsonMapper.treeToValue(payloadNode, TributeSubscriptionPayload::class.java)
        val user = userInfoService.applyPaidSubscription(payload.profile(), payload.subscription(change, ZoneId.systemDefault()))
        log.info("Tribute {} applied: user={}, period={}, expiresAt={}", change, user.telegramUserId, payload.period, user.subscriptionExpiresAt)
        return status(HttpStatus.OK)
    }

    private fun status(status: HttpStatus): ResponseEntity<Void> = ResponseEntity.status(status).build()
}
