package ai.moneymanager.domain.model.report

import ai.moneymanager.domain.model.CategoryType
import java.math.BigDecimal
import java.time.LocalDate

data class ComparisonReport(
    val previousMonthName: String,
    val currentMonthName: String,
    val year: String,
    val previousExpenseTotal: BigDecimal,
    val currentExpenseTotal: BigDecimal,
    val previousIncomeTotal: BigDecimal,
    val currentIncomeTotal: BigDecimal,
    val categoryComparisons: List<CategoryComparison>,
    val isEmpty: Boolean
)

data class CategoryComparison(
    val icon: String,
    val name: String,
    val previousAmount: BigDecimal,
    val currentAmount: BigDecimal
)

data class AnalyticsReport(
    val monthName: String,
    val year: Int,
    val totalExpense: BigDecimal,
    val totalIncome: BigDecimal,
    val operationCount: Int,
    val daysInMonth: Int,
    val topExpenses: List<CategoryTotal>,
    val maxExpense: MaxExpense?,
    val mostExpensiveDay: ExpensiveDay?,
    val isEmpty: Boolean
)

data class CategoryTotal(
    val icon: String,
    val name: String,
    val total: BigDecimal
)

data class MaxExpense(
    val amount: BigDecimal,
    val icon: String,
    val categoryName: String,
    val day: Int,
    val monthShort: String
)

data class ExpensiveDay(
    val day: Int,
    val monthName: String,
    val total: BigDecimal
)

data class MembersReport(
    val monthName: String,
    val year: Int,
    val expensesByMember: List<MemberTotal>,
    val incomesByMember: List<MemberTotal>,
    val totalExpense: BigDecimal,
    val totalIncome: BigDecimal,
    val isEmpty: Boolean
)

data class MemberTotal(
    val name: String,
    val total: BigDecimal
)

data class CategoryReport(
    val icon: String,
    val categoryName: String,
    val months: Int,
    val monthsData: List<CategoryMonthData>,
    val maxAmount: BigDecimal
)

data class CategoryMonthData(
    val label: String,
    val total: BigDecimal,
    val count: Int
)

/**
 * История операций за период: сами операции плюс посчитанные по ним итоги.
 * Telegram-рендер и REST API строятся поверх одних и тех же чисел.
 */
data class HistoryReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val operations: List<HistoryOperation>,
    val incomeByCategory: List<CategoryTotal>,
    val expenseByCategory: List<CategoryTotal>,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val isEmpty: Boolean
) {
    val balance: BigDecimal get() = totalIncome.subtract(totalExpense)
}

data class HistoryOperation(
    val id: String?,
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryName: String,
    val categoryIcon: String,
    val operationDate: LocalDate,
    val description: String?,
    val creatorId: Long
)