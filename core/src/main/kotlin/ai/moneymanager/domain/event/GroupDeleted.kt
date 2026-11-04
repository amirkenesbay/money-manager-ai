package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class GroupDeleted(
    val groupId: ObjectId,
    val actorId: Long,
    val groupName: String,
    val memberIds: Set<Long>,
)
