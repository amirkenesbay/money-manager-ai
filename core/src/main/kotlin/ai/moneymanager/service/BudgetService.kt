package ai.moneymanager.service

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.periodStartingAt
import org.bson.types.ObjectId
import ai.moneymanager.repository.FinanceOperationRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Service
class BudgetService(
    private val categoryService: CategoryService,
    private val financeOperationRepository: FinanceOperationRepository,
    private val clock: Clock,
) {
    fun budgets(group: MoneyGroup): List<Budget> = budgets(group, currentMonthPeriod(LocalDate.now(clock), group.monthStartDay))

    fun budgets(group: MoneyGroup, period: MonthPeriod): List<Budget> {
        val groupId = checkNotNull(group.id)
        val categories = categoryService.getBudgetCategories(groupId)
        if (categories.isEmpty()) return emptyList()
        val previous = periodStartingAt(period.start.minusMonths(1))
        val spent = spentByCategory(groupId, period)
        val spentBefore = if (categories.any { it.rollover }) spentByCategory(groupId, previous) else emptyMap()
        return categories.map { category ->
            val limit = checkNotNull(category.monthlyLimit)
            val carriedOver = if (category.rollover) limit - (spentBefore[category.id] ?: BigDecimal.ZERO) else BigDecimal.ZERO
            Budget(category, limit, spent[category.id] ?: BigDecimal.ZERO, period, carriedOver)
        }
    }

    private fun spentByCategory(groupId: ObjectId, period: MonthPeriod): Map<ObjectId, BigDecimal> =
        financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, period.start, period.end)
            .filter { it.type == CategoryType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, operations) -> operations.fold(BigDecimal.ZERO) { total, operation -> total + operation.amount } }
}
