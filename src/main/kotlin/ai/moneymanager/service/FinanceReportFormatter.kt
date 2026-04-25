package ai.moneymanager.service

import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryComparison
import ai.moneymanager.domain.model.report.CategoryMonthData
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR_WITH_BLANK_LINE
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.domain.model.report.MemberTotal
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private const val BAR_WIDTH = 12

@Component
class FinanceReportFormatter(
    private val localizationService: LocalizationService
) {

    fun formatComparison(report: ComparisonReport, language: String?): String = buildString {
        append(localizationService.t(
            "finance.report.comparison.title",
            language,
            report.previousMonthName, report.currentMonthName, report.year
        ))
        append(SECTION_SEPARATOR)

        if (report.isEmpty) {
            append("\n\n${localizationService.t("finance.report.comparison.empty", language)}")
            return@buildString
        }

        appendExpenseSection(report, language)
        appendIncomeSection(report, language)
        appendBalanceSection(report, language)
    }

    private fun StringBuilder.appendExpenseSection(report: ComparisonReport, language: String?) {
        append("\n\n")
        append(localizationService.t(
            "finance.report.comparison.expense",
            language,
            formatAmount(report.previousExpenseTotal), formatAmount(report.currentExpenseTotal)
        ))
        appendDelta(report.previousExpenseTotal, report.currentExpenseTotal)
        appendCategoryLines(report.categoryComparisons)
    }

    private fun StringBuilder.appendIncomeSection(report: ComparisonReport, language: String?) {
        append("\n\n")
        append(localizationService.t(
            "finance.report.comparison.income",
            language,
            formatAmount(report.previousIncomeTotal), formatAmount(report.currentIncomeTotal)
        ))
        appendDelta(report.previousIncomeTotal, report.currentIncomeTotal)
    }

    private fun StringBuilder.appendBalanceSection(report: ComparisonReport, language: String?) {
        val previousBalance = report.previousIncomeTotal.subtract(report.previousExpenseTotal)
        val currentBalance = report.currentIncomeTotal.subtract(report.currentExpenseTotal)
        val previousSign = if (previousBalance >= BigDecimal.ZERO) "+" else ""
        val currentSign = if (currentBalance >= BigDecimal.ZERO) "+" else ""

        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n")
        append(localizationService.t(
            "finance.report.comparison.balance",
            language,
            "$previousSign${formatAmount(previousBalance)}",
            "$currentSign${formatAmount(currentBalance)}"
        ))
        appendDelta(previousBalance, currentBalance)
    }

    private fun StringBuilder.appendCategoryLines(comparisons: List<CategoryComparison>) {
        if (comparisons.isEmpty()) return
        append("\n")
        comparisons.forEach { (icon, name, previousAmount, currentAmount) ->
            val percentChange = calculatePercentChange(previousAmount, currentAmount)
            val arrow = deltaArrow(percentChange)
            val warning = if (percentChange > 20) "  ⚠️" else ""
            append(
                "\n$icon $name  ${formatAmount(previousAmount)} → ${formatAmount(currentAmount)}  $arrow ${
                    abs(
                        percentChange
                    )
                }%$warning"
            )
        }
    }

    fun formatAnalytics(report: AnalyticsReport, language: String?): String = buildString {
        append(localizationService.t("finance.report.analytics.title", language, report.monthName, report.year))
        append(SECTION_SEPARATOR)

        if (report.isEmpty) {
            append("\n\n${localizationService.t("finance.report.analytics.empty", language)}")
            return@buildString
        }

        appendSummaryStats(report, language)
        appendDailyAverages(report, language)
        appendTopExpenses(report, language)
        appendMaxExpense(report, language)
        appendMostExpensiveDay(report, language)
    }

    private fun StringBuilder.appendSummaryStats(report: AnalyticsReport, language: String?) {
        append("\n\n")
        append(localizationService.t("finance.report.analytics.summary.title", language))
        append("\n")
        append(localizationService.t("finance.report.analytics.summary.expense", language, formatAmount(report.totalExpense)))
        append("\n")
        append(localizationService.t("finance.report.analytics.summary.income", language, formatAmount(report.totalIncome)))
        append("\n")
        append(localizationService.t("finance.report.analytics.summary.count", language, report.operationCount))
    }

    private fun StringBuilder.appendDailyAverages(report: AnalyticsReport, language: String?) {
        val days = BigDecimal.valueOf(report.daysInMonth.toLong())

        if (report.totalExpense.isPositive()) {
            val avg = report.totalExpense.divide(days, 0, RoundingMode.HALF_UP)
            append("\n\n")
            append(localizationService.t("finance.report.analytics.avg.expense", language, formatAmount(avg)))
        }
        if (report.totalIncome.isPositive()) {
            val avg = report.totalIncome.divide(days, 0, RoundingMode.HALF_UP)
            append("\n")
            append(localizationService.t("finance.report.analytics.avg.income", language, formatAmount(avg)))
        }
    }

    private fun StringBuilder.appendTopExpenses(report: AnalyticsReport, language: String?) {
        if (report.topExpenses.isEmpty()) return

        append("\n\n")
        append(localizationService.t("finance.report.analytics.top_expenses", language))
        report.topExpenses.forEachIndexed { index, (icon, name, total) ->
            val percent = percentOf(total, report.totalExpense)
            append("\n${index + 1}. $icon $name — ${formatAmount(total)} ($percent%)")
        }
    }

    private fun StringBuilder.appendMaxExpense(report: AnalyticsReport, language: String?) {
        val max = report.maxExpense ?: return
        append("\n\n")
        append(localizationService.t(
            "finance.report.analytics.max",
            language,
            formatAmount(max.amount), max.icon, max.categoryName, max.day, max.monthShort
        ))
    }

    private fun StringBuilder.appendMostExpensiveDay(report: AnalyticsReport, language: String?) {
        val day = report.mostExpensiveDay ?: return
        append("\n")
        append(localizationService.t(
            "finance.report.analytics.most_expensive_day",
            language,
            day.day, day.monthName, formatAmount(day.total)
        ))
    }

    fun formatMembers(report: MembersReport, language: String?): String = buildString {
        append(localizationService.t("finance.report.members.title", language, report.monthName, report.year))
        append(SECTION_SEPARATOR)

        if (report.isEmpty) {
            append("\n\n${localizationService.t("finance.report.members.empty", language)}")
            return@buildString
        }

        if (report.expensesByMember.isNotEmpty()) {
            append("\n\n")
            append(localizationService.t("finance.report.members.expense_section", language))
            appendMemberLines(report.expensesByMember, report.totalExpense)
        }

        if (report.incomesByMember.isNotEmpty()) {
            append("\n\n")
            append(localizationService.t("finance.report.members.income_section", language))
            appendMemberLines(report.incomesByMember, report.totalIncome)
        }

        appendBalanceLine(report.totalIncome, report.totalExpense, language)
    }

    private fun StringBuilder.appendMemberLines(members: List<MemberTotal>, total: BigDecimal) {
        members.forEach { (name, amount) ->
            val percent = percentOf(amount, total)
            append("\n  $name — ${formatAmount(amount)} ($percent%)")
        }
    }

    private fun StringBuilder.appendBalanceLine(
        totalIncome: BigDecimal,
        totalExpense: BigDecimal,
        language: String?
    ) {
        val balance = totalIncome.subtract(totalExpense)
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n")
        append(localizationService.t("finance.report.members.balance", language, "$sign${formatAmount(balance)}"))
    }

    fun formatCategory(report: CategoryReport, language: String?): String = buildString {
        append(localizationService.t(
            "finance.report.category.title_with_period",
            language,
            report.icon, report.categoryName, report.months
        ))
        append(SECTION_SEPARATOR)
        append("\n")

        appendBarChart(report.monthsData, report.maxAmount, language)
        appendCategorySummary(report.monthsData, language)
    }

    private fun StringBuilder.appendBarChart(
        monthsData: List<CategoryMonthData>,
        maxAmount: BigDecimal,
        language: String?
    ) {
        monthsData.forEach { (label, total, _) ->
            val filled = if (maxAmount.isPositive()) {
                total.multiply(BigDecimal.valueOf(BAR_WIDTH.toLong()))
                    .divide(maxAmount, 0, RoundingMode.HALF_UP).toInt()
            } else 0
            val empty = BAR_WIDTH - filled
            val bar = "█".repeat(filled) + "░".repeat(empty)
            val maxMarker = if (total.compareTo(maxAmount) == 0 && total.isPositive()) {
                localizationService.t("finance.report.category.max_marker", language)
            } else ""
            append("\n$label  $bar ${formatAmount(total)}$maxMarker")
        }
    }

    private fun StringBuilder.appendCategorySummary(monthsData: List<CategoryMonthData>, language: String?) {
        val totalSum = monthsData.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.total) }
        val average = if (monthsData.isNotEmpty()) {
            totalSum.divide(BigDecimal.valueOf(monthsData.size.toLong()), 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val totalOperations = monthsData.sumOf { it.count }

        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n")
        append(localizationService.t("finance.report.category.average", language, formatAmount(average)))
        append("\n")
        append(localizationService.t("finance.report.category.total_operations", language, totalOperations))

        if (monthsData.size >= 2) {
            val lastMonth = monthsData.last().total
            val previousMonth = monthsData[monthsData.size - 2].total
            val trendKey = when {
                lastMonth > previousMonth -> "finance.report.category.trend.up"
                lastMonth < previousMonth -> "finance.report.category.trend.down"
                else -> "finance.report.category.trend.stable"
            }
            val trend = localizationService.t(trendKey, language)
            append("\n")
            append(localizationService.t("finance.report.category.trend", language, trend))
        }
    }

    private fun StringBuilder.appendDelta(previous: BigDecimal, current: BigDecimal) {
        if (previous.isZero() && current.isZero()) return
        val percent = calculatePercentChange(previous, current)
        val arrow = deltaArrow(percent)
        append(" ($arrow ${abs(percent)}%)")
    }

    private fun calculatePercentChange(previous: BigDecimal, current: BigDecimal): Int =
        if (previous.isPositive()) {
            current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP).toInt()
        } else if (current.isPositive()) 100 else 0

    private fun deltaArrow(percent: Int): String = when {
        percent > 0 -> "↑"
        percent < 0 -> "↓"
        else -> "→"
    }

    private fun percentOf(part: BigDecimal, total: BigDecimal): BigDecimal =
        if (total.isPositive()) {
            part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

    private fun BigDecimal.isPositive(): Boolean = compareTo(BigDecimal.ZERO) > 0
    private fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0
}
