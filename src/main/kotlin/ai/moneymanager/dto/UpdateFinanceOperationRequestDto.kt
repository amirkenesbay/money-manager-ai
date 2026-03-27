package ai.moneymanager.dto

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateFinanceOperationRequestDto(
    val telegramUserId: Long,
    val groupId: ObjectId? = null,
    val categoryId: ObjectId? = null,
    val day: LocalDate? = null,
    val amount: BigDecimal? = null,
    val currency: CurrencyType? = null,
    val description: String? = null
)
