package ai.moneymanager.web.me

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.SubscriptionTier
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant

data class MeResponse(
    val userId: Long,
    val firstName: String?,
    val username: String?,
    val language: String,
    val timezone: String?,
    val activeGroupId: ObjectId?,
    val seenHints: Set<AppHint>,
    val subscription: SubscriptionResponse,
)

data class SubscriptionResponse(
    val tier: SubscriptionTier,
    val expiresAt: Instant?,
    val limits: LimitsResponse,
    val freeLimits: LimitsResponse,
    val paidLimits: LimitsResponse,
    val price: PriceResponse,
    val subscribeUrl: String,
)

data class PriceResponse(
    val amount: BigDecimal,
    val currency: Currency,
)

data class LimitsResponse(
    val aiRequestsPerDay: Int,
    val categoriesPerType: Int,
    val ownedSharedGroups: Int?,
    val activeNotifications: Int?,
    val historyDaysBack: Int?,
)

data class MarkHintRequest(
    val hint: AppHint,
)

data class UpdateMeRequest(
    val language: String?,
    val timezone: String?,
)
