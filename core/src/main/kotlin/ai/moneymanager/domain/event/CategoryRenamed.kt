package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class CategoryRenamed(
    val groupId: ObjectId,
    val actorId: Long,
    val icon: String?,
    val oldName: String,
    val newName: String,
)
