package ai.moneymanager.web.voice

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.question.AnswerCard
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

data class VoiceResponse(
    val operations: List<VoiceOperationResponse>,
    val answer: AnswerCard? = null,
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
    val repeat: VoiceRepeatResponse? = null,
)

data class VoiceRepeatResponse(
    val unit: RecurrenceUnit,
    val interval: Int,
)
