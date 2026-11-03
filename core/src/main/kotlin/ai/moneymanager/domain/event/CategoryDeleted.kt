package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class CategoryDeleted(
    val groupId: ObjectId,
    val categoryId: ObjectId,
    val actorId: Long,
    val icon: String?,
    val name: String,
)
