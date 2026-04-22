package ai.moneymanager.service

import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR_WITH_BLANK_LINE
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Service
class FinanceHistoryService(
    private val financeOperationRepository: FinanceOperationRepository
) {

    fun generateReport(groupId: ObjectId, startDate: LocalDate, endDate: LocalDate): String {
        val operations = financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, startDate, endDate)

        val header = buildReportHeader(startDate, endDate)

        if (operations.isEmpty()) {
            return "$header\n\nНет операций за выбранный период"
        }

        val incomes = operations.filter { it.type == CategoryType.INCOME }
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }
        val totalIncome = sumAmounts(incomes)
        val totalExpense = sumAmounts(expenses)

        return buildString {
            append(header)
            append(SECTION_SEPARATOR)
            appendSection("📈 Доходы:", incomes, totalIncome)
            appendSection("📉 Расходы:", expenses, totalExpense)
            appendBalanceLine(totalIncome, totalExpense)
        }
    }

    private fun buildReportHeader(startDate: LocalDate, endDate: LocalDate): String {
        val title = buildReportTitle(startDate, endDate)
        val dateRange = "📅 ${startDate.format(dateFormatter)} – ${endDate.format(dateFormatter)}"
        return "$title\n$dateRange"
    }

    private fun buildReportTitle(startDate: LocalDate, endDate: LocalDate): String {
        val isFullMonth = startDate.dayOfMonth == 1
            && startDate.year == endDate.year
            && startDate.month == endDate.month
            && endDate == startDate.withDayOfMonth(startDate.lengthOfMonth())

        return if (isFullMonth) {
            val monthName = startDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.of("ru"))
                .replaceFirstChar { it.uppercaseChar() }
            "📋 Отчёт за $monthName ${startDate.year}"
        } else {
            "📋 Отчёт за ${startDate.format(dateFormatter)} – ${endDate.format(dateFormatter)}"
        }
    }

    private fun StringBuilder.appendSection(
        title: String,
        operations: List<FinanceOperationEntity>,
        total: BigDecimal
    ) {
        if (operations.isEmpty()) return
        append("\n\n$title")
        appendCategoryLines(operations)
        append("\n─────────────────────")
        append("\nИтого: ${formatAmount(total)}")
    }

    private fun StringBuilder.appendCategoryLines(operations: List<FinanceOperationEntity>) {
        operations
            .groupBy { (it.categoryIcon ?: "📌") to it.categoryName }
            .map { (key, ops) -> Triple(key.first, key.second, sumAmounts(ops)) }
            .sortedByDescending { it.third }
            .forEach { (icon, name, total) ->
                append("\n$icon $name — ${formatAmount(total)}")
            }
    }

    private fun StringBuilder.appendBalanceLine(totalIncome: BigDecimal, totalExpense: BigDecimal) {
        val balance = totalIncome.subtract(totalExpense)
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n💰 Баланс: $sign${formatAmount(balance)}")
    }

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }
}