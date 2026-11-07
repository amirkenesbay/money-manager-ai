package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

enum class SubscriptionChange {
    STARTED,
    RENEWED,
    CANCELLED,
}

data class PaidSubscription(
    val change: SubscriptionChange,
    val period: SubscriptionPeriod?,
    val expiresAt: LocalDateTime,
    val price: BigDecimal,
    val currency: String,
)
