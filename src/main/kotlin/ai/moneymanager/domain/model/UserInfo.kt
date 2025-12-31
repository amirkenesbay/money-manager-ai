package ai.moneymanager.domain.model

import org.bson.types.ObjectId

data class UserInfo(
    val id: ObjectId? = null,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val telegramUserId: Long?,
    val languageCode: String?,
    val activeGroupId: ObjectId? = null,
    val groupIds: Set<ObjectId> = emptySet()
)
