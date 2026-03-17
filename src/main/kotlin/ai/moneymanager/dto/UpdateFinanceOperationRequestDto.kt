package ai.moneymanager.dto

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import org.bson.types.ObjectId
import java.math.BigDecimal

data class UpdateFinanceOperationRequestDto(
    val telegramUserId: Long,
    val groupId: ObjectId? = null,
    val categoryId: ObjectId? = null,
    val day: DayType? = null,
    val amount: BigDecimal? = null,
    val currency: CurrencyType? = null,
    val description: String? = null
)
