package ai.moneymanager.domain.model.report

import java.math.BigDecimal

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