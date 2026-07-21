package ai.moneymanager.service

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.bold
import ai.moneymanager.chat.reply.common.italic
import ai.moneymanager.chat.reply.common.code
import ai.moneymanager.chat.reply.common.blockquote
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.pre
import ai.moneymanager.chat.reply.common.progressBar
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
        currency: Currency,
        language: String?,
        typeFilter: CategoryType? = null,
        categoryFilter: String? = null
    ): String {
        val operations = financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, startDate, endDate)
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { categoryFilter == null || matchesCategoryFilter(it, categoryFilter) }

        val header = buildReportHeader(startDate, endDate, language)

        if (operations.isEmpty()) {
            return "$header\n\n${localizationService.t("finance.history.empty", language)}"
        }
        if (categoryFilter != null) {
            return buildItemizedReport(header, operations, currency, language)
        }

        val incomes = operations.filter { it.type == CategoryType.INCOME }
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }
        val totalIncome = sumAmounts(incomes)
        val totalExpense = sumAmounts(expenses)

        return buildString {
            append(header)
            appendSection(localizationService.t("finance.history.section.income", language), incomes, totalIncome, currency, language)
            appendSection(localizationService.t("finance.history.section.expense", language), expenses, totalExpense, currency, language)
            appendBalanceLine(totalIncome, totalExpense, currency, language)
        }
    }

    /** Поимённый список операций (с датами) — для запросов с фильтром по категории/ключевому слову. */
    private fun buildItemizedReport(
        header: String,
        operations: List<FinanceOperationEntity>,
        currency: Currency,
        language: String?
    ): String = buildString {
        append(header)
        append("\n")
        operations.forEach { operation ->
            val icon = operation.categoryIcon ?: DEFAULT_CATEGORY_ICON
            append("\n${italic(operation.operationDate.format(shortDateFormatter))} $icon ${escapeHtml(operation.categoryName)}")
            append(" ${code(formatSignedAmount(operation.type, operation.amount, currency))}")
            append(escapeHtml(formatDescriptionSuffix(operation.description)))
        }
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.total", language, formatAmount(sumAmounts(operations), currency)))))
    }

    private fun matchesCategoryFilter(operation: FinanceOperationEntity, filter: String): Boolean =
        matchesEntityName(operation.categoryName, filter) ||
            operation.description?.contains(filter, ignoreCase = true) == true

    private fun buildReportHeader(startDate: LocalDate, endDate: LocalDate, language: String?): String {
        // Диапазонный заголовок уже содержит обе даты — отдельная строка с периодом нужна только месячному.
        if (!isFullMonth(startDate, endDate)) {
            return bold(
                localizationService.t(
                    "finance.history.title.range",
                    language,
                    startDate.format(dateFormatter),
                    endDate.format(dateFormatter)
                )
            )
        }
        val monthName = startDate.month
            .getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language))
            .replaceFirstChar { it.uppercaseChar() }
        val title = localizationService.t("finance.history.title.month", language, monthName, startDate.year.toString())
        val dateRange = localizationService.t(
            "finance.history.date_range",
            language,
            startDate.format(dateFormatter),
            endDate.format(dateFormatter)
        )
        return "${bold(title)}\n$dateRange"
    }

    private fun isFullMonth(startDate: LocalDate, endDate: LocalDate): Boolean =
        startDate.dayOfMonth == 1
            && startDate.year == endDate.year
            && startDate.month == endDate.month
            && endDate == startDate.withDayOfMonth(startDate.lengthOfMonth())

    private fun StringBuilder.appendSection(
        title: String,
        operations: List<FinanceOperationEntity>,
        total: BigDecimal,
        currency: Currency,
        language: String?
    ) {
        if (operations.isEmpty()) return
        append("\n\n$title")
        appendCategoryLines(operations, currency)
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.total", language, formatAmount(total, currency)))))
    }

    private fun StringBuilder.appendCategoryLines(operations: List<FinanceOperationEntity>, currency: Currency) {
        val totals = operations
            .groupBy { (it.categoryIcon ?: DEFAULT_CATEGORY_ICON) to it.categoryName }
            .map { (key, ops) -> Triple(key.first, key.second, sumAmounts(ops)) }
            .sortedByDescending { it.third }
        val maxTotal = totals.maxOfOrNull { it.third } ?: BigDecimal.ZERO
        val labelWidth = totals.maxOfOrNull { (icon, name, _) -> "$icon $name".length } ?: 0
        val rows = totals.joinToString("\n") { (icon, name, total) ->
            val label = "$icon ${escapeHtml(name)}".padEnd(labelWidth)
            "$label  ${progressBar(total, maxTotal)} ${formatAmount(total, currency)}"
        }
        append("\n${pre(rows)}")
    }

    private fun StringBuilder.appendBalanceLine(
        totalIncome: BigDecimal,
        totalExpense: BigDecimal,
        currency: Currency,
        language: String?
    ) {
        val balance = totalIncome.subtract(totalExpense)
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.balance", language, "$sign${formatAmount(balance, currency)}"))))
    }

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }

    private fun localeFor(language: String?): Locale =
        Locale.of(language ?: LocalizationService.FALLBACK_LANGUAGE)
}
