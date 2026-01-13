package ai.moneymanager.domain.model

import org.bson.types.ObjectId

/**
 * Категория для учета доходов/расходов
 */
data class Category(
    val id: ObjectId? = null,
    val name: String,
    val icon: String? = null,
    val type: CategoryType,
    val groupId: ObjectId
)

/**
 * Тип категории
 */
enum class CategoryType {
    /**
     * Категория расходов
     */
    EXPENSE,

    /**
     * Категория доходов
     */
    INCOME
}