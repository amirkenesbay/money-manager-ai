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
}