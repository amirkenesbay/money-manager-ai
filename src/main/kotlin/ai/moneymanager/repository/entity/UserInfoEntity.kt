package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.SubscriptionTier
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
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionExpiresAt: LocalDateTime? = null,
    val auditInfo: AuditInfo = AuditInfo()
)
