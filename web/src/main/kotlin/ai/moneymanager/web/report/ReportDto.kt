package ai.moneymanager.web.report

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.web.finance.CategoryAmountResponse
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class AnalyticsReportResponse(
    val month: YearMonth,
    val from: LocalDate,
    val to: LocalDate,
    val currency: Currency,
    val income: BigDecimal,
    val expense: BigDecimal,
    val operationCount: Int,
    val daysInMonth: Int,
    val topExpenses: List<CategoryAmountResponse>,
    val maxExpense: MaxExpenseResponse?,
    val mostExpensiveDay: ExpensiveDayResponse?,
)

data class MaxExpenseResponse(
    val amount: BigDecimal,
    val icon: String,
    val categoryName: String,
    val date: LocalDate,
)

data class ExpensiveDayResponse(
    val date: LocalDate,
    val total: BigDecimal,
)

data class ComparisonReportResponse(
    val month: YearMonth,
    val from: LocalDate,
    val to: LocalDate,
    val previousMonth: YearMonth,
    val currency: Currency,
    val income: MonthPair,
    val expense: MonthPair,
    val categories: List<CategoryComparisonResponse>,
)

data class MonthPair(
    val previous: BigDecimal,
    val current: BigDecimal,
)

data class CategoryComparisonResponse(
    val icon: String,
    val name: String,
    val previous: BigDecimal,
    val current: BigDecimal,
)

data class MembersReportResponse(
    val month: YearMonth,
    val from: LocalDate,
    val to: LocalDate,
    val currency: Currency,
    val income: BigDecimal,
    val expense: BigDecimal,
    val incomeByMember: List<MemberAmountResponse>,
    val expenseByMember: List<MemberAmountResponse>,
)

data class MemberAmountResponse(
    val name: String,
    val total: BigDecimal,
)

data class CategoryReportResponse(
    val categoryId: ObjectId,
    val icon: String,
    val name: String,
    val currency: Currency,
    val months: List<CategoryMonthResponse>,
)

data class CategoryMonthResponse(
    val month: YearMonth,
    val total: BigDecimal,
    val count: Int,
)
