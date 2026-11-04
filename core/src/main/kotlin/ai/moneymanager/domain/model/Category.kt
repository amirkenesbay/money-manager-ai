package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.math.BigDecimal

const val DEFAULT_CATEGORY_ICON: String = "📌"
const val MAX_CATEGORY_NAME_LENGTH = 50

data class Category(
    val id: ObjectId? = null,
    val name: String,
    val icon: String? = null,
    val type: CategoryType,
    val groupId: ObjectId,
    val monthlyLimit: BigDecimal? = null,
    val rollover: Boolean = false
)

enum class CategoryType {
    EXPENSE,

    INCOME
}
