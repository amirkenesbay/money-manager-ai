package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.GroupNoticeLevel
import ai.moneymanager.domain.model.SubscriptionPeriod
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "money_manager_user")
data class UserInfoEntity(
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
    val groupNotices: Map<String, GroupNoticeLevel> = emptyMap(),
    val recurringHeadsUp: Boolean = true,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionExpiresAt: LocalDateTime? = null,
    val subscriptionPeriod: SubscriptionPeriod? = null,
    val dailyReminderSeeded: Boolean = false,
    val access: UserAccess = UserAccess.REGULAR,
    val botBlockedAt: LocalDateTime? = null,
    val auditInfo: AuditInfo = AuditInfo()
)
