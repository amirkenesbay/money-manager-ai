package ai.moneymanager.web.report

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.MemberTotal
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.web.finance.CategoryAmountResponse
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ReportResponseFactory {
    fun analytics(report: AnalyticsReport, period: MonthPeriod, currency: Currency) = AnalyticsReportResponse(
        month = period.month,
        from = period.start,
        to = period.end,
        currency = currency,
        income = report.totalIncome,
        expense = report.totalExpense,
        operationCount = report.operationCount,
        daysInMonth = report.daysInMonth,
        topExpenses = report.topExpenses.map { it.toResponse() },
        maxExpense = report.maxExpense?.let { MaxExpenseResponse(it.amount, it.icon, it.categoryName, period.dateOf(it.day)) },
        mostExpensiveDay = report.mostExpensiveDay?.let { ExpensiveDayResponse(period.dateOf(it.day), it.total) },
    )

    fun comparison(report: ComparisonReport, period: MonthPeriod, currency: Currency) = ComparisonReportResponse(
        month = period.month,
        from = period.start,
        to = period.end,
        previousMonth = period.month.minusMonths(1),
        currency = currency,
        income = MonthPair(report.previousIncomeTotal, report.currentIncomeTotal),
        expense = MonthPair(report.previousExpenseTotal, report.currentExpenseTotal),
        categories = report.categoryComparisons.map {
            CategoryComparisonResponse(it.icon, it.name, it.previousAmount, it.currentAmount)
        },
    )

    fun members(report: MembersReport, period: MonthPeriod, currency: Currency) = MembersReportResponse(
        month = period.month,
        from = period.start,
        to = period.end,
        currency = currency,
        income = report.totalIncome,
        expense = report.totalExpense,
        incomeByMember = report.incomesByMember.map { it.toResponse() },
        expenseByMember = report.expensesByMember.map { it.toResponse() },
    )

    fun category(report: CategoryReport, categoryId: ObjectId, currency: Currency) = CategoryReportResponse(
        categoryId = categoryId,
        icon = report.icon,
        name = report.categoryName,
        currency = currency,
        months = report.monthsData.map { CategoryMonthResponse(it.month, it.total, it.count) },
    )

    private fun CategoryTotal.toResponse() = CategoryAmountResponse(icon, name, total)

    private fun MemberTotal.toResponse() = MemberAmountResponse(name, total)
}
