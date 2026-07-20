package ai.moneymanager.service

import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FinanceOperationService(
    private val financeOperationRepository: FinanceOperationRepository
) {

    private val log = LoggerFactory.getLogger(FinanceOperationService::class.java)

    fun calculateBalance(groupId: ObjectId, initialBalance: BigDecimal): BalanceBreakdown {
        val ops = financeOperationRepository.findByGroupId(groupId)
        val income = ops.filter { it.type == CategoryType.INCOME }
            .fold(BigDecimal.ZERO) { acc, op -> acc + op.amount }
        val expense = ops.filter { it.type == CategoryType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, op -> acc + op.amount }
        return BalanceBreakdown(
            initial = initialBalance,
            income = income,
            expense = expense,
            total = initialBalance + income - expense
        )
    }

    fun save(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        categoryId: ObjectId,
        categoryName: String,
        categoryIcon: String?,
        operationDate: LocalDate,
        description: String?
    ): FinanceOperationEntity {
        val entity = FinanceOperationEntity(
            groupId = groupId,
            creatorId = creatorId,
            type = type,
            amount = BigDecimal.valueOf(amount),
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            operationDate = operationDate,
            description = description
        )

        val saved = financeOperationRepository.save(entity)
        log.info("Saved finance operation: type={}, amount={}, category={}, groupId={}", type, amount, categoryName, groupId)
        return saved
    }

    fun getOperation(groupId: ObjectId, operationId: ObjectId): FinanceOperationEntity? =
        financeOperationRepository.findByGroupIdAndIdIn(groupId, listOf(operationId)).firstOrNull()

    fun updateAmount(groupId: ObjectId, operationId: ObjectId, amount: BigDecimal): FinanceOperationEntity? =
        updateOperation(groupId, operationId) { it.copy(amount = amount) }

    fun updateType(groupId: ObjectId, operationId: ObjectId, type: CategoryType): FinanceOperationEntity? =
        updateOperation(groupId, operationId) { it.copy(type = type) }

    fun updateDate(groupId: ObjectId, operationId: ObjectId, date: LocalDate): FinanceOperationEntity? =
        updateOperation(groupId, operationId) { it.copy(operationDate = date) }

    fun updateDescription(groupId: ObjectId, operationId: ObjectId, description: String?): FinanceOperationEntity? =
        updateOperation(groupId, operationId) { it.copy(description = description) }

    fun updateCategory(
        groupId: ObjectId,
        operationId: ObjectId,
        categoryId: ObjectId,
        categoryName: String,
        categoryIcon: String?
    ): FinanceOperationEntity? = updateOperation(groupId, operationId) {
        it.copy(categoryId = categoryId, categoryName = categoryName, categoryIcon = categoryIcon)
    }

    fun deleteOperation(groupId: ObjectId, operationId: ObjectId): Boolean {
        val existing = getOperation(groupId, operationId) ?: return false
        financeOperationRepository.delete(existing)
        log.info("Deleted finance operation: id={}, groupId={}", operationId, groupId)
        return true
    }

    private fun updateOperation(
        groupId: ObjectId,
        operationId: ObjectId,
        mutate: (FinanceOperationEntity) -> FinanceOperationEntity
    ): FinanceOperationEntity? {
        val existing = getOperation(groupId, operationId) ?: return null
        val saved = financeOperationRepository.save(mutate(existing))
        log.info("Updated finance operation: id={}, groupId={}", operationId, groupId)
        return saved
    }
}