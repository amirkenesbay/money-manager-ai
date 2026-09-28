package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.time.LocalDate

data class OperationFilter(
    val groupId: ObjectId,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val type: CategoryType? = null,
    val categoryId: ObjectId? = null,
)
