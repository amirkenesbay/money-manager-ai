package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class CategoryCreated(
    val groupId: ObjectId,
    val actorId: Long,
    val icon: String?,
    val name: String,
)
