package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.OperationTotals
import ai.moneymanager.domain.model.report.HistoryReport
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.UserInfoService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class FinanceResponseFactory(
    private val userInfoService: UserInfoService,
) {
    fun balance(group: MoneyGroup, breakdown: BalanceBreakdown) = BalanceResponse(
        currency = group.currency,
        initial = breakdown.initial,
        income = breakdown.income,
        expense = breakdown.expense,
        total = breakdown.total,
    )

    fun monthSummary(group: MoneyGroup, period: MonthPeriod, report: HistoryReport) = MonthSummaryResponse(
        month = period.month,
        from = period.start,
        to = period.end,
        currency = group.currency,
        income = report.totalIncome,
        expense = report.totalExpense,
        net = report.balance,
        operationCount = report.operations.size,
        expenseByCategory = report.expenseByCategory.map { CategoryAmountResponse(it.icon, it.name, it.total) },
        dailyExpense = dailyExpense(report),
    )

    private fun dailyExpense(report: HistoryReport): List<DailyAmountResponse> =
        report.operations
            .filter { it.type == CategoryType.EXPENSE }
            .groupBy { it.operationDate }
            .toSortedMap()
            .map { (date, operations) -> DailyAmountResponse(date, operations.sumOf { it.amount }) }

    fun operationsPage(page: Page<FinanceOperationEntity>, totals: OperationTotals? = null): OperationsPageResponse {
        val creatorNames = userInfoService.displayNames(page.content.map { it.creatorId }.toSet())
        return OperationsPageResponse(
            items = page.content.map { it.toResponse(creatorNames.getValue(it.creatorId)) },
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
            totals = totals?.let { OperationTotalsResponse(it.income, it.expense, it.count) },
        )
    }

    fun operation(operation: FinanceOperationEntity): OperationResponse =
        operation.toResponse(userInfoService.displayNames(setOf(operation.creatorId)).getValue(operation.creatorId))

    private fun FinanceOperationEntity.toResponse(creatorName: String) = OperationResponse(
        id = checkNotNull(id),
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
        operationDate = operationDate,
        description = description,
        creatorId = creatorId,
        creatorName = creatorName,
    )
}
