package ai.moneymanager.domain.model

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import java.math.BigDecimal

data class FinanceOperation(
    val id: ObjectId,
    val telegramUserId: ObjectId,
    val day: DayType,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null,
    val auditInfo: AuditInfo = AuditInfo()
) {
    companion object {
        fun createFinanceOperation(
            telegramUserId: ObjectId,
            day: DayType,
            amount: BigDecimal,
            operationType: OperationType,
            currency: CurrencyType,
            description: String? = null,
            auditInfo: AuditInfo = AuditInfo()
        ) = FinanceOperation(
            id = ObjectId(),
            telegramUserId = telegramUserId,
            day = day,
            amount = amount,
            operationType = operationType,
            currency = currency,
            description = description,
            auditInfo = auditInfo,
        )
    }
}