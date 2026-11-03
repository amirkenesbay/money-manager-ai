package ai.moneymanager.domain.model

import org.bson.types.ObjectId

data class CategoryRule(
    val id: ObjectId,
    val merchantKey: String,
    val categoryId: ObjectId,
)
