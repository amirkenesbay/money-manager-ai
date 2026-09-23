package ai.moneymanager.domain.model

import org.bson.types.ObjectId

data class Category(
    val id: ObjectId? = null,
    val name: String,
    val icon: String? = null,
    val type: CategoryType,
    val groupId: ObjectId
)

enum class CategoryType {
    EXPENSE,

    INCOME
}
