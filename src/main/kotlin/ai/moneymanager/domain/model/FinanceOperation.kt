package ai.moneymanager.domain.model

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import org.bson.types.ObjectId
import java.math.BigDecimal

data class FinanceOperation(
    val id: ObjectId? = null,
    val telegramUserId: Long,
    val groupId: ObjectId? = null,
    val categoryId: ObjectId? = null,
    val day: DayType,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null
) {
    init {
        require(amount > BigDecimal.ZERO) {
            "Amount must be greater than 0"
        }
    }

    companion object {
        fun create(
            telegramUserId: Long,
            groupId: ObjectId? = null,
            categoryId: ObjectId?,
            day: DayType,
            amount: BigDecimal,
            operationType: OperationType,
            currency: CurrencyType,
            description: String?
        ): FinanceOperation =
            FinanceOperation(
                telegramUserId = telegramUserId,
                groupId = groupId,
                categoryId = categoryId,
                day = day,
                amount = amount,
                operationType = operationType,
                currency = currency,
                description = description?.trim()?.takeIf { it.isNotBlank() }
            )
    }
}