package ai.moneymanager.service

import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryComparison
import ai.moneymanager.domain.model.report.CategoryMonthData
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.domain.model.report.MemberTotal
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs

private const val SEPARATOR = "\n━━━━━━━━━━━━━━━━━━━━━"
private const val SEPARATOR_WITH_BLANK_LINE = "\n\n━━━━━━━━━━━━━━━━━━━━━"
private const val BAR_WIDTH = 12

@Component
class FinanceReportFormatter {

    fun formatComparison(report: ComparisonReport): String = buildString {
        append("📊 Сравнение: ${report.previousMonthName} → ${report.currentMonthName} ${report.year}")
        append(SEPARATOR)

        if (report.isEmpty) {
            append("\n\nНет операций за выбранный период")
            return@buildString
        }

        appendExpenseSection(report)
        appendIncomeSection(report)
        appendBalanceSection(report)
    }

    private fun StringBuilder.appendExpenseSection(report: ComparisonReport) {
        append("\n\n📉 Расходы: ${formatAmount(report.previousExpenseTotal)} → ${formatAmount(report.currentExpenseTotal)}")
        appendDelta(report.previousExpenseTotal, report.currentExpenseTotal)
        appendCategoryLines(report.categoryComparisons)
    }

    private fun StringBuilder.appendIncomeSection(report: ComparisonReport) {
        append("\n\n📈 Доходы: ${formatAmount(report.previousIncomeTotal)} → ${formatAmount(report.currentIncomeTotal)}")
        appendDelta(report.previousIncomeTotal, report.currentIncomeTotal)
    }

    private fun StringBuilder.appendBalanceSection(report: ComparisonReport) {
        val previousBalance = report.previousIncomeTotal.subtract(report.previousExpenseTotal)
        val currentBalance = report.currentIncomeTotal.subtract(report.currentExpenseTotal)
        val previousSign = if (previousBalance >= BigDecimal.ZERO) "+" else ""
        val currentSign = if (currentBalance >= BigDecimal.ZERO) "+" else ""

        append(SEPARATOR_WITH_BLANK_LINE)
        append("\n💰 Баланс: $previousSign${formatAmount(previousBalance)} → $currentSign${formatAmount(currentBalance)}")
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

    fun formatAnalytics(report: AnalyticsReport): String = buildString {
        append("📈 Аналитика за ${report.monthName} ${report.year}")
        append(SEPARATOR)

        if (report.isEmpty) {
            append("\n\nНет операций за этот месяц")
            return@buildString
        }

        appendSummaryStats(report)
        appendDailyAverages(report)
        appendTopExpenses(report)
        appendMaxExpense(report)
        appendMostExpensiveDay(report)
    }

    private fun StringBuilder.appendSummaryStats(report: AnalyticsReport) {
        append("\n\n📊 Общие цифры:")
        append("\n📉 Расходы: ${formatAmount(report.totalExpense)}")
        append("\n📈 Доходы: ${formatAmount(report.totalIncome)}")
        append("\n🔢 Операций: ${report.operationCount}")
    }

    private fun StringBuilder.appendDailyAverages(report: AnalyticsReport) {
        val days = BigDecimal.valueOf(report.daysInMonth.toLong())

        if (report.totalExpense.isPositive()) {
            val avg = report.totalExpense.divide(days, 0, RoundingMode.HALF_UP)
            append("\n\n📉 Средний расход в день: ${formatAmount(avg)}")
        }
        if (report.totalIncome.isPositive()) {
            val avg = report.totalIncome.divide(days, 0, RoundingMode.HALF_UP)
            append("\n📈 Средний доход в день: ${formatAmount(avg)}")
        }
    }

    private fun StringBuilder.appendTopExpenses(report: AnalyticsReport) {
        if (report.topExpenses.isEmpty()) return

        append("\n\n🏆 Топ-3 расходов:")
        report.topExpenses.forEachIndexed { index, (icon, name, total) ->
            val percent = percentOf(total, report.totalExpense)
            append("\n${index + 1}. $icon $name — ${formatAmount(total)} ($percent%)")
        }
    }

    private fun StringBuilder.appendMaxExpense(report: AnalyticsReport) {
        val max = report.maxExpense ?: return
        append("\n\n💸 Макс. трата: ${formatAmount(max.amount)} (${max.icon} ${max.categoryName}, ${max.day} ${max.monthShort})")
    }

    private fun StringBuilder.appendMostExpensiveDay(report: AnalyticsReport) {
        val day = report.mostExpensiveDay ?: return
        append("\n📅 Самый затратный день: ${day.day} ${day.monthName} — ${formatAmount(day.total)}")
    }

    fun formatMembers(report: MembersReport): String = buildString {
        append("👥 По участникам — ${report.monthName} ${report.year}")
        append(SEPARATOR)

        if (report.isEmpty) {
            append("\n\nНет операций за этот месяц")
            return@buildString
        }

        if (report.expensesByMember.isNotEmpty()) {
            append("\n\n📉 Расходы:")
            appendMemberLines(report.expensesByMember, report.totalExpense)
        }

        if (report.incomesByMember.isNotEmpty()) {
            append("\n\n📈 Доходы:")
            appendMemberLines(report.incomesByMember, report.totalIncome)
        }

        appendBalanceLine(report.totalIncome, report.totalExpense)
    }

    private fun StringBuilder.appendMemberLines(members: List<MemberTotal>, total: BigDecimal) {
        members.forEach { (name, amount) ->
            val percent = percentOf(amount, total)
            append("\n  $name — ${formatAmount(amount)} ($percent%)")
        }
    }

    private fun StringBuilder.appendBalanceLine(totalIncome: BigDecimal, totalExpense: BigDecimal) {
        val balance = totalIncome.subtract(totalExpense)
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append(SEPARATOR_WITH_BLANK_LINE)
        append("\n💰 Баланс группы: $sign${formatAmount(balance)}")
    }

    fun formatCategory(report: CategoryReport): String = buildString {
        append("${report.icon} ${report.categoryName} — за ${report.months} месяцев")
        append(SEPARATOR)
        append("\n")

        appendBarChart(report.monthsData, report.maxAmount)
        appendCategorySummary(report.monthsData)
    }

    private fun StringBuilder.appendBarChart(monthsData: List<CategoryMonthData>, maxAmount: BigDecimal) {
        monthsData.forEach { (label, total, _) ->
            val filled = if (maxAmount.isPositive()) {
                total.multiply(BigDecimal.valueOf(BAR_WIDTH.toLong()))
                    .divide(maxAmount, 0, RoundingMode.HALF_UP).toInt()
            } else 0
            val empty = BAR_WIDTH - filled
            val bar = "█".repeat(filled) + "░".repeat(empty)
            val maxMarker = if (total.compareTo(maxAmount) == 0 && total.isPositive()) "  ← макс" else ""
            append("\n$label  $bar ${formatAmount(total)}$maxMarker")
        }
    }

    private fun StringBuilder.appendCategorySummary(monthsData: List<CategoryMonthData>) {
        val totalSum = monthsData.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.total) }
        val average = if (monthsData.isNotEmpty()) {
            totalSum.divide(BigDecimal.valueOf(monthsData.size.toLong()), 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val totalOperations = monthsData.sumOf { it.count }

        append(SEPARATOR_WITH_BLANK_LINE)
        append("\nСреднее: ${formatAmount(average)}/мес")
        append("\nВсего операций: $totalOperations")

        if (monthsData.size >= 2) {
            val lastMonth = monthsData.last().total
            val previousMonth = monthsData[monthsData.size - 2].total
            val trend = when {
                lastMonth > previousMonth -> "↑ растёт"
                lastMonth < previousMonth -> "↓ снижается"
                else -> "→ стабильно"
            }
            append("\nТренд: $trend")
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

    private fun formatAmount(amount: BigDecimal): String {
        val format = DecimalFormat("#,##0", DecimalFormatSymbols().apply { groupingSeparator = ' ' })
        return "${format.format(amount)}₸"
    }

    private fun BigDecimal.isPositive(): Boolean = compareTo(BigDecimal.ZERO) > 0
    private fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0
}