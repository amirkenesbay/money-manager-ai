package ai.moneymanager.service

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR_WITH_BLANK_LINE
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val MONTH_DIVIDER = "─────────────────────"

@Service
class FinanceHistoryService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val localizationService: LocalizationService
) {

    fun getRecentOperations(groupId: ObjectId, limit: Int): List<FinanceOperationEntity> =
        financeOperationRepository
            .findByGroupIdOrderByOperationDateDescAuditInfoCreatedAtDesc(
                groupId,
                PageRequest.of(0, limit)
            )

    fun generateReport(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        language: String?
    ): String {
        val operations = financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, startDate, endDate)

        val header = buildReportHeader(startDate, endDate, language)

        if (operations.isEmpty()) {
            return "$header\n\n${localizationService.t("finance.history.empty", language)}"
        }

        val incomes = operations.filter { it.type == CategoryType.INCOME }
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }
        val totalIncome = sumAmounts(incomes)
        val totalExpense = sumAmounts(expenses)

        return buildString {
            append(header)
            append(SECTION_SEPARATOR)
            appendSection(localizationService.t("finance.history.section.income", language), incomes, totalIncome, language)
            appendSection(localizationService.t("finance.history.section.expense", language), expenses, totalExpense, language)
            appendBalanceLine(totalIncome, totalExpense, language)
        }
    }

    private fun buildReportHeader(startDate: LocalDate, endDate: LocalDate, language: String?): String {
        val title = buildReportTitle(startDate, endDate, language)
        val dateRange = localizationService.t(
            "finance.history.date_range",
            language,
            startDate.format(dateFormatter),
            endDate.format(dateFormatter)
        )
        return "$title\n$dateRange"
    }

    private fun buildReportTitle(startDate: LocalDate, endDate: LocalDate, language: String?): String {
        val isFullMonth = startDate.dayOfMonth == 1
            && startDate.year == endDate.year
            && startDate.month == endDate.month
            && endDate == startDate.withDayOfMonth(startDate.lengthOfMonth())

        return if (isFullMonth) {
            val monthName = startDate.month
                .getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language))
                .replaceFirstChar { it.uppercaseChar() }
            localizationService.t("finance.history.title.month", language, monthName, startDate.year)
        } else {
            localizationService.t(
                "finance.history.title.range",
                language,
                startDate.format(dateFormatter),
                endDate.format(dateFormatter)
            )
        }
    }

    private fun StringBuilder.appendSection(
        title: String,
        operations: List<FinanceOperationEntity>,
        total: BigDecimal,
        language: String?
    ) {
        if (operations.isEmpty()) return
        append("\n\n$title")
        appendCategoryLines(operations)
        append("\n$MONTH_DIVIDER")
        append("\n${localizationService.t("finance.history.total", language, formatAmount(total))}")
    }

    private fun StringBuilder.appendCategoryLines(operations: List<FinanceOperationEntity>) {
        operations
            .groupBy { (it.categoryIcon ?: DEFAULT_CATEGORY_ICON) to it.categoryName }
            .map { (key, ops) -> Triple(key.first, key.second, sumAmounts(ops)) }
            .sortedByDescending { it.third }
            .forEach { (icon, name, total) ->
                append("\n$icon $name — ${formatAmount(total)}")
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
        append("\n${localizationService.t("finance.history.balance", language, "$sign${formatAmount(balance)}")}")
    }

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }

    private fun localeFor(language: String?): Locale =
        Locale.of(language ?: LocalizationService.FALLBACK_LANGUAGE)
}
