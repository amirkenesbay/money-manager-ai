package ai.moneymanager.web.voice

import ai.moneymanager.domain.model.CategoryType
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

data class VoiceResponse(
    val operations: List<VoiceOperationResponse>,
)

data class VoiceOperationResponse(
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId?,
    val categoryName: String?,
    val categoryIcon: String?,
    val isNewCategory: Boolean,
    val operationDate: LocalDate,
    val description: String?,
)
