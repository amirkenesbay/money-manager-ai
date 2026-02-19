package ai.moneymanager.dto

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import org.bson.types.ObjectId
import java.math.BigDecimal

data class UpdateFinanceOperationRequestDto(
    val telegramUserId: Long,
    val groupId: ObjectId?,
    val categoryId: ObjectId?,
    val day: DayType,
    val amount: BigDecimal,
    val currency: CurrencyType,
    val description: String? = null
)
