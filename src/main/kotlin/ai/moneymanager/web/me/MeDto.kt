package ai.moneymanager.web.me

import ai.moneymanager.domain.model.SubscriptionTier
import org.bson.types.ObjectId
import java.time.Instant

data class MeResponse(
    val userId: Long,
    val firstName: String?,
    val username: String?,
    val language: String,
    val timezone: String?,
    val activeGroupId: ObjectId?,
    val subscription: SubscriptionResponse,
)

data class SubscriptionResponse(
    val tier: SubscriptionTier,
    val expiresAt: Instant?,
    val limits: LimitsResponse,
)

data class LimitsResponse(
    val aiRequestsPerDay: Int,
    val categoriesPerType: Int,
    val ownedSharedGroups: Int?,
    val activeNotifications: Int?,
    val historyDaysBack: Int?,
)

data class UpdateMeRequest(
    val language: String,
)
