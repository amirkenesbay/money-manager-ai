package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class CategoryUpdated(
    val groupId: ObjectId,
    val actorId: Long,
    val oldIcon: String?,
    val oldName: String,
    val newIcon: String?,
    val newName: String,
)
