package ai.moneymanager.dto

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import org.bson.types.ObjectId
import java.math.BigDecimal

data class CreateFinanceOperationRequestDto(
    val telegramUserId: Long,
    val groupId: ObjectId? = null,
    val categoryId: ObjectId? = null,
    val day: DayType,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null
)
