package ai.moneymanager.repository.entity

import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "money_manager_user")
data class UserInfoEntity(
    @Id
    val id: ObjectId? = null,
    val username: String?,
    val firstName: String?,
    val lastName: String?,

    @Indexed(unique = true)
    val telegramUserId: Long,

    val languageCode: String?,
    val activeGroupId: ObjectId? = null,
    val groupIds: Set<ObjectId> = emptySet(),
    val auditInfo: AuditInfo = AuditInfo()
)