package ai.moneymanager.dto

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDateTime

data class HistoryFinanceOperationDto(
    val telegramUserId: Long,
    val amount: BigDecimal,
    val currency: CurrencyType,
    val operationType: OperationType,
    val categoryId: ObjectId?,
    val day: DayType,
    val description: String?,
    val createdAt: LocalDateTime?
)