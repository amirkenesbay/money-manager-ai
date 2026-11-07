package ai.moneymanager.web.subscription

import ai.moneymanager.domain.model.PaidSubscription
import ai.moneymanager.domain.model.SubscriptionChange
import ai.moneymanager.domain.model.SubscriptionPeriod
import ai.moneymanager.domain.model.TelegramProfile
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private const val MINOR_UNITS_SCALE = 2

private val CHANGES = mapOf(
    "new_subscription" to SubscriptionChange.STARTED,
    "renewed_subscription" to SubscriptionChange.RENEWED,
    "cancelled_subscription" to SubscriptionChange.CANCELLED,
)

private val PERIODS = mapOf(
    "monthly" to SubscriptionPeriod.MONTH,
    "quarterly" to SubscriptionPeriod.QUARTER,
    "yearly" to SubscriptionPeriod.YEAR,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TributeEvent(
    val name: String? = null,
    val payload: JsonNode? = null,
) {
    val change: SubscriptionChange? get() = CHANGES[name]
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TributeSubscriptionPayload(
    @JsonProperty("telegram_user_id") val telegramUserId: Long,
    @JsonProperty("telegram_username") val telegramUsername: String? = null,
    val period: String? = null,
    val price: Long = 0,
    val currency: String = "",
    @JsonProperty("expires_at") val expiresAt: Instant,
) {
    fun profile(): TelegramProfile =
        TelegramProfile(telegramUserId = telegramUserId, username = telegramUsername, firstName = null, lastName = null, languageCode = null)

    fun subscription(change: SubscriptionChange, zone: ZoneId): PaidSubscription = PaidSubscription(
        change = change,
        period = period?.let { PERIODS[it.lowercase()] },
        expiresAt = LocalDateTime.ofInstant(expiresAt, zone),
        price = BigDecimal.valueOf(price, MINOR_UNITS_SCALE),
        currency = currency.uppercase(),
    )
}
