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
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.HistoryOperation
import ai.moneymanager.domain.model.report.HistoryReport
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

    /**
     * Данные истории без форматирования — используются и Telegram-рендером, и REST API.
     */
    fun buildHistoryReport(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        typeFilter: CategoryType? = null,
        categoryFilter: String? = null
    ): HistoryReport {
        val operations = findOperations(groupId, startDate, endDate, typeFilter, categoryFilter)
        val incomes = operations.filter { it.type == CategoryType.INCOME }
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }

        return HistoryReport(
            startDate = startDate,
            endDate = endDate,
            operations = operations.map { it.toHistoryOperation() },
            incomeByCategory = groupByCategory(incomes),
            expenseByCategory = groupByCategory(expenses),
            totalIncome = sumAmounts(incomes),
            totalExpense = sumAmounts(expenses),
            isEmpty = operations.isEmpty()
        )
    }

    fun generateReport(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: Currency,
        language: String?,
        typeFilter: CategoryType? = null,
        categoryFilter: String? = null
    ): String {
        val report = buildHistoryReport(groupId, startDate, endDate, typeFilter, categoryFilter)
        val header = buildReportHeader(startDate, endDate, language)

        if (report.isEmpty) {
            return "$header\n\n${localizationService.t("finance.history.empty", language)}"
        }
        if (categoryFilter != null) {
            return buildItemizedReport(header, report, currency, language)
        }

        return buildString {
            append(header)
            appendSection(localizationService.t("finance.history.section.income", language), report.incomeByCategory, report.totalIncome, currency, language)
            appendSection(localizationService.t("finance.history.section.expense", language), report.expenseByCategory, report.totalExpense, currency, language)
            appendBalanceLine(report.balance, currency, language)
        }
    }

    private fun findOperations(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
        typeFilter: CategoryType?,
        categoryFilter: String?
    ): List<FinanceOperationEntity> =
        financeOperationRepository
            .findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, startDate, endDate)
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { categoryFilter == null || matchesCategoryFilter(it, categoryFilter) }

    /** Суммы по категориям, от большей к меньшей. */
    private fun groupByCategory(operations: List<FinanceOperationEntity>): List<CategoryTotal> =
        operations
            .groupBy { (it.categoryIcon ?: DEFAULT_CATEGORY_ICON) to it.categoryName }
            .map { (key, ops) -> CategoryTotal(icon = key.first, name = key.second, total = sumAmounts(ops)) }
            .sortedByDescending { it.total }

    private fun FinanceOperationEntity.toHistoryOperation(): HistoryOperation = HistoryOperation(
        id = id?.toHexString(),
        type = type,
        amount = amount,
        categoryName = categoryName,
        categoryIcon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
        operationDate = operationDate,
        description = description,
        creatorId = creatorId
    )

    /** Поимённый список операций (с датами) — для запросов с фильтром по категории/ключевому слову. */
    private fun buildItemizedReport(
        header: String,
        report: HistoryReport,
        currency: Currency,
        language: String?
    ): String = buildString {
        append(header)
        append("\n")
        report.operations.forEach { operation ->
            append("\n${italic(operation.operationDate.format(shortDateFormatter))} ${operation.categoryIcon} ${escapeHtml(operation.categoryName)}")
            append(" ${code(formatSignedAmount(operation.type, operation.amount, currency))}")
            append(escapeHtml(formatDescriptionSuffix(operation.description)))
        }
        val total = report.totalIncome.add(report.totalExpense)
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.total", language, formatAmount(total, currency)))))
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
        categoryTotals: List<CategoryTotal>,
        total: BigDecimal,
        currency: Currency,
        language: String?
    ) {
        if (categoryTotals.isEmpty()) return
        append("\n\n$title")
        appendCategoryLines(categoryTotals, currency)
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.total", language, formatAmount(total, currency)))))
    }

    private fun StringBuilder.appendCategoryLines(categoryTotals: List<CategoryTotal>, currency: Currency) {
        val maxTotal = categoryTotals.maxOfOrNull { it.total } ?: BigDecimal.ZERO
        val labelWidth = categoryTotals.maxOfOrNull { "${it.icon} ${it.name}".length } ?: 0
        val rows = categoryTotals.joinToString("\n") { categoryTotal ->
            val label = "${categoryTotal.icon} ${escapeHtml(categoryTotal.name)}".padEnd(labelWidth)
            "$label  ${progressBar(categoryTotal.total, maxTotal)} ${formatAmount(categoryTotal.total, currency)}"
        }
        append("\n${pre(rows)}")
    }

    private fun StringBuilder.appendBalanceLine(
        balance: BigDecimal,
        currency: Currency,
        language: String?
    ) {
        val sign = if (balance >= BigDecimal.ZERO) "+" else ""
        append("\n\n")
        append(blockquote(bold(localizationService.t("finance.history.balance", language, "$sign${formatAmount(balance, currency)}"))))
    }

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }

    private fun localeFor(language: String?): Locale =
        Locale.of(language ?: LocalizationService.FALLBACK_LANGUAGE)
}
