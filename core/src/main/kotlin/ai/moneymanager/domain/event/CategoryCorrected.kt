package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class CategoryCorrected(
    val groupId: ObjectId,
    val description: String?,
    val categoryId: ObjectId,
)
