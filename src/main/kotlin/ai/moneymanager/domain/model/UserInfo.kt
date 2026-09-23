package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.time.LocalDateTime

data class UserInfo(
    val id: ObjectId? = null,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val telegramUserId: Long?,
    val languageCode: String?,
    val language: String? = null,
    val activeGroupId: ObjectId? = null,
    val groupIds: Set<ObjectId> = emptySet(),
    val timezone: String? = null,
    val onboardingCompleted: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionExpiresAt: LocalDateTime? = null
) {
    fun hasActivePaidSubscription(now: LocalDateTime = LocalDateTime.now()): Boolean =
        subscriptionTier == SubscriptionTier.PAID && subscriptionExpiresAt?.isAfter(now) == true
}
