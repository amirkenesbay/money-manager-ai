package ai.moneymanager.web.me

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.SubscriptionPeriod
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
    val mutedDigests: Set<DigestKind>,
    val recurringHeadsUp: Boolean,
    val subscription: SubscriptionResponse,
)

data class SubscriptionResponse(
    val tier: SubscriptionTier,
    val expiresAt: Instant?,
    val limits: LimitsResponse,
    val freeLimits: LimitsResponse,
    val paidLimits: LimitsResponse,
    val prices: List<PriceResponse>,
    val subscribeUrl: String,
)

data class PriceResponse(
    val period: SubscriptionPeriod,
    val amount: BigDecimal,
    val currency: Currency,
)

data class LimitsResponse(
    val aiRequestsPerDay: Int,
    val categoriesPerType: Int,
    val ownedSharedGroups: Int?,
    val activeNotifications: Int?,
    val historyDaysBack: Int?,
    val budgets: Int?,
    val recurringPayments: Int?,
    val imports: Int?,
)

data class DigestSettingRequest(
    val kind: DigestKind,
    val muted: Boolean,
)

data class MarkHintRequest(
    val hint: AppHint,
)

data class UpdateMeRequest(
    val language: String? = null,
    val timezone: String? = null,
    val recurringHeadsUp: Boolean? = null,
)
