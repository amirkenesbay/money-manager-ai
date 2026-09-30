package ai.moneymanager.service

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.HistoryOperation
import ai.moneymanager.domain.model.report.HistoryReport
import ai.moneymanager.domain.text.matchesEntityName
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FinanceHistoryService(
    private val financeOperationRepository: FinanceOperationRepository
) {
    fun getRecentOperations(groupId: ObjectId, limit: Int): List<FinanceOperationEntity> =
        financeOperationRepository
            .findByGroupIdOrderByOperationDateDescAuditInfoCreatedAtDesc(
                groupId,
                PageRequest.of(0, limit)
            )

    fun buildHistoryReport(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        typeFilter: CategoryType? = null,
        categoryFilter: String? = null
    ): HistoryReport {
        val operations = findOperations(groupId, startDate, endDate, typeFilter, categoryFilter)
        val incomes = operations.filter { it.type == CategoryType.INCOME }
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }

        return HistoryReport(
            startDate = startDate,
            endDate = endDate,
            operations = operations.map { it.toHistoryOperation() },
            incomeByCategory = groupByCategory(incomes),
            expenseByCategory = groupByCategory(expenses),
            totalIncome = sumAmounts(incomes),
            totalExpense = sumAmounts(expenses),
            isEmpty = operations.isEmpty()
        )
    }

    private fun findOperations(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        typeFilter: CategoryType?,
        categoryFilter: String?
    ): List<FinanceOperationEntity> =
        financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, startDate, endDate)
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { categoryFilter == null || matchesCategoryFilter(it, categoryFilter) }

    private fun groupByCategory(operations: List<FinanceOperationEntity>): List<CategoryTotal> =
        operations
            .groupBy { (it.categoryIcon ?: DEFAULT_CATEGORY_ICON) to it.categoryName }
            .map { (key, ops) -> CategoryTotal(icon = key.first, name = key.second, total = sumAmounts(ops)) }
            .sortedByDescending { it.total }

    private fun FinanceOperationEntity.toHistoryOperation(): HistoryOperation = HistoryOperation(
        id = id?.toHexString(),
        type = type,
        amount = amount,
        categoryName = categoryName,
        categoryIcon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
        operationDate = operationDate,
        description = description,
        creatorId = creatorId
    )

    private fun matchesCategoryFilter(operation: FinanceOperationEntity, filter: String): Boolean =
        matchesEntityName(operation.categoryName, filter) ||
            operation.description?.contains(filter, ignoreCase = true) == true

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }
}
