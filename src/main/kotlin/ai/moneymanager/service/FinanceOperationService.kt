package ai.moneymanager.service

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class FinanceOperationService(
    private val financeOperationRepository: FinanceOperationRepository
) {
    fun createFinanceOperationIncome(
        telegramUserId: ObjectId,
        groupId: ObjectId,
        day: DayType,
        amount: BigDecimal,
        operationType: OperationType,
        currency: CurrencyType,
        auditInfo: AuditInfo,
        description: String? = null
    ) {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }

        val financeOperationEntity = FinanceOperationEntity(
            telegramUserId = telegramUserId,
            groupId = groupId,
            day = day,
            amount = amount,
            operationType = operationType,
            currency = currency,
            description = description,
            auditInfo = auditInfo
        )
        financeOperationRepository.save(financeOperationEntity)
    }
    fun createFinanceOperationExpense(
        telegramUserId: ObjectId,
        groupId: ObjectId,
        day: DayType,
        amount: BigDecimal,
        operationType: OperationType,
        currency: CurrencyType,
        auditInfo: AuditInfo,
        description: String? = null
    ) {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }

        val financeOperationEntity = FinanceOperationEntity(
            telegramUserId = telegramUserId,
            groupId = groupId,
            day = day,
            amount = amount,
            operationType = operationType,
            currency = currency,
            description = description,
            auditInfo = auditInfo
        )
        financeOperationRepository.save(financeOperationEntity)
    }

}