package ai.moneymanager.service

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.currentMonthPeriod
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
    fun budgets(group: MoneyGroup): List<Budget> {
        val groupId = checkNotNull(group.id)
        val categories = categoryService.getBudgetCategories(groupId)
        if (categories.isEmpty()) return emptyList()
        val period = currentMonthPeriod(LocalDate.now(clock), group.monthStartDay)
        val spentByCategory = financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, period.start, period.end)
            .filter { it.type == CategoryType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, operations) -> operations.fold(BigDecimal.ZERO) { total, operation -> total + operation.amount } }
        return categories.map { category ->
            Budget(category, checkNotNull(category.monthlyLimit), spentByCategory[category.id] ?: BigDecimal.ZERO, period)
        }
    }
}
