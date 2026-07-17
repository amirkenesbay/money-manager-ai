package ai.moneymanager.service

import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryComparison
import ai.moneymanager.domain.model.report.CategoryMonthData
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.chat.reply.common.bold
import ai.moneymanager.chat.reply.common.pre
import ai.moneymanager.chat.reply.common.italic
import ai.moneymanager.chat.reply.common.code
import ai.moneymanager.chat.reply.common.blockquote
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.progressBar
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.domain.model.report.MemberTotal
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private const val TOP_CHANGES_LIMIT = 3
private const val TREND_UP_MARKER = "🔺"
private const val TREND_DOWN_MARKER = "🔻"

@Component
class FinanceReportFormatter(
    private val localizationService: LocalizationService
) {

    fun formatComparison(report: ComparisonReport, language: String?): String = buildString {
        append(bold(localizationService.t(
            "finance.report.comparison.title",
            language,
            report.previousMonthName, report.currentMonthName, report.year
        )))

        if (report.isEmpty) {
            append("\n\n${localizationService.t("finance.report.comparison.empty", language)}")
            return@buildString
        }

        val maxValue = listOf(
            report.previousExpenseTotal, report.currentExpenseTotal,
            report.previousIncomeTotal, report.currentIncomeTotal
        ).max()

        appendComparisonSection(
            localizationService.t("finance.report.comparison.section.expense", language),
            report.previousMonthName, report.previousExpenseTotal,
            report.currentMonthName, report.currentExpenseTotal,
            maxValue
        )
        appendComparisonSection(
            localizationService.t("finance.report.comparison.section.income", language),
            report.previousMonthName, report.previousIncomeTotal,
            report.currentMonthName, report.currentIncomeTotal,
            maxValue
        )
        appendBalanceSection(report, language)
        appendTopChanges(report.categoryComparisons, language)
    }

    private fun StringBuilder.appendComparisonSection(
        title: String,
        previousLabel: String,
        previousValue: BigDecimal,
        currentLabel: String,
        currentValue: BigDecimal,
        maxValue: BigDecimal
    ) {
        val labelWidth = maxOf(previousLabel.length, currentLabel.length)
        val rows = listOf(previousLabel to previousValue, currentLabel to currentValue)
            .joinToString("\n") { (label, value) ->
                "${label.padEnd(labelWidth)} ${progressBar(value, maxValue)} ${formatAmount(value)}"
            }
        append("\n\n$title\n${pre(rows)}")
    }

    private fun StringBuilder.appendBalanceSection(report: ComparisonReport, language: String?) {
        val previousBalance = report.previousIncomeTotal.subtract(report.previousExpenseTotal)
        val currentBalance = report.currentIncomeTotal.subtract(report.currentExpenseTotal)
        val previousSign = if (previousBalance >= BigDecimal.ZERO) "+" else ""
        val currentSign = if (currentBalance >= BigDecimal.ZERO) "+" else ""

        append("\n\n")
        append(blockquote(bold(localizationService.t(
            "finance.report.comparison.balance",
            language,
            "$previousSign${formatAmount(previousBalance)}",
            "$currentSign${formatAmount(currentBalance)}"
        ))))
    }

    private fun StringBuilder.appendTopChanges(comparisons: List<CategoryComparison>, language: String?) {
        val top = comparisons
            .filter { it.currentAmount.compareTo(it.previousAmount) != 0 }
            .take(TOP_CHANGES_LIMIT)
        if (top.isEmpty()) return
        append("\n\n")
        append(localizationService.t("finance.report.comparison.top_changes", language))
        top.forEach { (icon, name, previousAmount, currentAmount) ->
            val diff = currentAmount.subtract(previousAmount)
            val marker = if (diff > BigDecimal.ZERO) TREND_UP_MARKER else TREND_DOWN_MARKER
            val sign = if (diff > BigDecimal.ZERO) "+" else "−"
            append("\n$marker $icon ${escapeHtml(name)}  $sign${formatAmount(diff.abs())}")
        }
    }

    fun formatAnalytics(report: AnalyticsReport, language: String?): String = buildString {
        append(bold(localizationService.t("finance.report.analytics.title", language, report.monthName, report.year.toString())))

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
        val maxTotal = report.topExpenses.maxOf { it.total }
        report.topExpenses.forEach { (icon, name, total) ->
            val percent = percentOf(total, report.totalExpense)
            append("\n$icon ${escapeHtml(name)} · ${italic("$percent%")}\n${code("${progressBar(total, maxTotal)} ${formatAmount(total)}")}")
        }
    }

    private fun StringBuilder.appendMaxExpense(report: AnalyticsReport, language: String?) {
        val max = report.maxExpense ?: return
        append("\n\n")
        append(blockquote(localizationService.t(
            "finance.report.analytics.max",
            language,
            formatAmount(max.amount), max.icon, escapeHtml(max.categoryName), max.day, max.monthShort
        )))
    }

    private fun StringBuilder.appendMostExpensiveDay(report: AnalyticsReport, language: String?) {
        val day = report.mostExpensiveDay ?: return
        append("\n")
        append(blockquote(localizationService.t(
            "finance.report.analytics.most_expensive_day",
            language,
            day.day, day.monthName, formatAmount(day.total)
        )))
    }

    fun formatMembers(report: MembersReport, language: String?): String = buildString {
        append(bold(localizationService.t("finance.report.members.title", language, report.monthName, report.year.toString())))

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
        val maxAmount = members.maxOfOrNull { it.total } ?: BigDecimal.ZERO
        members.forEach { (name, amount) ->
            val percent = percentOf(amount, total)
            append("\n${escapeHtml(name)} · ${italic("$percent%")}\n${code("${progressBar(amount, maxAmount)} ${formatAmount(amount)}")}")
        }
    }

    private fun StringBuilder.appendBalanceLine(
        totalIncome: BigDecimal,
        totalExpense: BigDecimal,
        language: String?
    ) {
        val balance = totalIncome.subtract(totalExpense)
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.report.members.balance", language, "$sign${formatAmount(balance)}"))))
    }

    fun formatCategory(report: CategoryReport, language: String?): String = buildString {
        append(bold(localizationService.t(
            "finance.report.category.title_with_period",
            language,
            report.icon, escapeHtml(report.categoryName), report.months
        )))
        append("\n")

        appendBarChart(report.monthsData, report.maxAmount, language)
        appendCategorySummary(report.monthsData, language)
    }

    private fun StringBuilder.appendBarChart(
        monthsData: List<CategoryMonthData>,
        maxAmount: BigDecimal,
        language: String?
    ) {
        val labelWidth = monthsData.maxOfOrNull { it.label.length } ?: 0
        val rows = monthsData.joinToString("\n") { (label, total, _) ->
            val maxMarker = if (total.compareTo(maxAmount) == 0 && total.isPositive()) {
                localizationService.t("finance.report.category.max_marker", language)
            } else ""
            "${label.padEnd(labelWidth)} ${progressBar(total, maxAmount)} ${formatAmount(total)}$maxMarker"
        }
        append("\n${pre(rows)}")
    }

    private fun StringBuilder.appendCategorySummary(monthsData: List<CategoryMonthData>, language: String?) {
        val totalSum = monthsData.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.total) }
        val average = if (monthsData.isNotEmpty()) {
            totalSum.divide(BigDecimal.valueOf(monthsData.size.toLong()), 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val totalOperations = monthsData.sumOf { it.count }

        append("\n\n")
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

    private fun percentOf(part: BigDecimal, total: BigDecimal): BigDecimal =
        if (total.isPositive()) {
            part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

    private fun BigDecimal.isPositive(): Boolean = compareTo(BigDecimal.ZERO) > 0
}
