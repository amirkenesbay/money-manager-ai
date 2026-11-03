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
    val miniAppIntroduced: Boolean = false,
    val seenHints: Set<AppHint> = emptySet(),
    val mutedDigests: Set<DigestKind> = emptySet(),
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionExpiresAt: LocalDateTime? = null
) {
    fun hasActivePaidSubscription(now: LocalDateTime = LocalDateTime.now()): Boolean =
        subscriptionTier == SubscriptionTier.PAID && subscriptionExpiresAt?.isAfter(now) == true
}

private const val USER_ID_LABEL = "ID"
private const val USERNAME_PREFIX = "@"

fun formatUserDisplayName(userInfo: UserInfo?, fallbackId: Long): String {
    val fullName = listOfNotNull(userInfo?.firstName, userInfo?.lastName).joinToString(" ")
    return when {
        fullName.isNotEmpty() -> fullName
        !userInfo?.username.isNullOrEmpty() -> "$USERNAME_PREFIX${userInfo?.username}"
        else -> "$USER_ID_LABEL $fallbackId"
    }
}
