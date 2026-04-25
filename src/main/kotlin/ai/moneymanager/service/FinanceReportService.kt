package ai.moneymanager.service

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.report.AnalyticsReport
import ai.moneymanager.domain.model.report.CategoryComparison
import ai.moneymanager.domain.model.report.CategoryMonthData
import ai.moneymanager.domain.model.report.CategoryReport
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.model.report.ComparisonReport
import ai.moneymanager.domain.model.report.ExpensiveDay
import ai.moneymanager.domain.model.report.MaxExpense
import ai.moneymanager.domain.model.report.MemberTotal
import ai.moneymanager.domain.model.report.MembersReport
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Service
class FinanceReportService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val formatter: FinanceReportFormatter
) {

    fun generateComparisonReport(groupId: ObjectId, previousMonthStart: LocalDate, language: String?): String {
        val report = buildComparisonReport(groupId, previousMonthStart, language)
        return formatter.formatComparison(report, language)
    }

    fun generateAnalyticsReport(groupId: ObjectId, monthStart: LocalDate, language: String?): String {
        val report = buildAnalyticsReport(groupId, monthStart, language)
        return formatter.formatAnalytics(report, language)
    }

    fun generateMembersReport(
        groupId: ObjectId,
        monthStart: LocalDate,
        memberNames: Map<Long, String>,
        language: String?
    ): String {
        val report = buildMembersReport(groupId, monthStart, memberNames, language)
        return formatter.formatMembers(report, language)
    }

    fun generateCategoryReport(
        groupId: ObjectId,
        categoryId: ObjectId,
        categoryIcon: String?,
        categoryName: String,
        language: String?,
        months: Int = 6
    ): String {
        val report = buildCategoryReport(groupId, categoryId, categoryIcon, categoryName, months, language)
        return formatter.formatCategory(report, language)
    }


    private fun buildComparisonReport(
        groupId: ObjectId,
        previousMonthStart: LocalDate,
        language: String?
    ): ComparisonReport {
        val currentMonthStart = previousMonthStart.plusMonths(1)

        val previousOps = findMonthOperations(groupId, previousMonthStart)
        val currentOps = findMonthOperations(groupId, currentMonthStart)

        val previousExpenses = previousOps.filterByType(CategoryType.EXPENSE)
        val currentExpenses = currentOps.filterByType(CategoryType.EXPENSE)

        return ComparisonReport(
            previousMonthName = monthNameShort(previousMonthStart, language),
            currentMonthName = monthNameShort(currentMonthStart, language),
            year = formatYearRange(previousMonthStart, currentMonthStart),
            previousExpenseTotal = sumAmounts(previousExpenses),
            currentExpenseTotal = sumAmounts(currentExpenses),
            previousIncomeTotal = sumAmounts(previousOps.filterByType(CategoryType.INCOME)),
            currentIncomeTotal = sumAmounts(currentOps.filterByType(CategoryType.INCOME)),
            categoryComparisons = buildCategoryComparisons(previousExpenses, currentExpenses),
            isEmpty = previousOps.isEmpty() && currentOps.isEmpty()
        )
    }

    private fun buildCategoryComparisons(
        previousExpenses: List<FinanceOperationEntity>,
        currentExpenses: List<FinanceOperationEntity>
    ): List<CategoryComparison> {
        val previousByCategory = previousExpenses.groupByCategory()
        val currentByCategory = currentExpenses.groupByCategory()

        return (previousByCategory.keys + currentByCategory.keys)
            .distinct()
            .map { (icon, name) ->
                CategoryComparison(
                    icon = icon,
                    name = name,
                    previousAmount = previousByCategory[icon to name] ?: BigDecimal.ZERO,
                    currentAmount = currentByCategory[icon to name] ?: BigDecimal.ZERO
                )
            }
            .sortedByDescending { it.currentAmount.subtract(it.previousAmount).abs() }
    }

    private fun buildAnalyticsReport(groupId: ObjectId, monthStart: LocalDate, language: String?): AnalyticsReport {
        val operations = findMonthOperations(groupId, monthStart)
        val expenses = operations.filterByType(CategoryType.EXPENSE)
        val totalExpense = sumAmounts(expenses)

        return AnalyticsReport(
            monthName = monthNameFull(monthStart, language),
            year = monthStart.year,
            totalExpense = totalExpense,
            totalIncome = sumAmounts(operations.filterByType(CategoryType.INCOME)),
            operationCount = operations.size,
            daysInMonth = monthStart.lengthOfMonth(),
            topExpenses = buildTopExpenses(expenses, limit = 3),
            maxExpense = buildMaxExpense(expenses, language),
            mostExpensiveDay = buildMostExpensiveDay(expenses, language),
            isEmpty = operations.isEmpty()
        )
    }

    private fun buildTopExpenses(expenses: List<FinanceOperationEntity>, limit: Int): List<CategoryTotal> =
        expenses.groupByCategory()
            .map { (key, total) -> CategoryTotal(icon = key.first, name = key.second, total = total) }
            .sortedByDescending { it.total }
            .take(limit)

    private fun buildMaxExpense(expenses: List<FinanceOperationEntity>, language: String?): MaxExpense? {
        val max = expenses.maxByOrNull { it.amount } ?: return null
        return MaxExpense(
            amount = max.amount,
            icon = max.categoryIcon ?: DEFAULT_CATEGORY_ICON,
            categoryName = max.categoryName,
            day = max.operationDate.dayOfMonth,
            monthShort = monthNameShort(max.operationDate, language)
        )
    }

    private fun buildMostExpensiveDay(expenses: List<FinanceOperationEntity>, language: String?): ExpensiveDay? {
        val entry = expenses
            .groupBy { it.operationDate }
            .mapValues { (_, ops) -> sumAmounts(ops) }
            .maxByOrNull { it.value } ?: return null

        return ExpensiveDay(
            day = entry.key.dayOfMonth,
            monthName = monthNameFull(entry.key, language),
            total = entry.value
        )
    }

    private fun buildMembersReport(
        groupId: ObjectId,
        monthStart: LocalDate,
        memberNames: Map<Long, String>,
        language: String?
    ): MembersReport {
        val operations = findMonthOperations(groupId, monthStart)
        val expenses = operations.filterByType(CategoryType.EXPENSE)
        val incomes = operations.filterByType(CategoryType.INCOME)

        return MembersReport(
            monthName = monthNameFull(monthStart, language),
            year = monthStart.year,
            expensesByMember = buildMemberTotals(expenses, memberNames),
            incomesByMember = buildMemberTotals(incomes, memberNames),
            totalExpense = sumAmounts(expenses),
            totalIncome = sumAmounts(incomes),
            isEmpty = operations.isEmpty()
        )
    }

    private fun buildMemberTotals(
        operations: List<FinanceOperationEntity>,
        memberNames: Map<Long, String>
    ): List<MemberTotal> =
        operations.groupBy { it.creatorId }
            .map { (creatorId, ops) ->
                MemberTotal(
                    name = memberNames[creatorId] ?: "ID $creatorId",
                    total = sumAmounts(ops)
                )
            }
            .sortedByDescending { it.total }

    private fun buildCategoryReport(
        groupId: ObjectId,
        categoryId: ObjectId,
        categoryIcon: String?,
        categoryName: String,
        months: Int,
        language: String?
    ): CategoryReport {
        val monthsData = loadCategoryMonthsData(groupId, categoryId, months, language)

        return CategoryReport(
            icon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
            categoryName = categoryName,
            months = months,
            monthsData = monthsData,
            maxAmount = monthsData.maxOfOrNull { it.total } ?: BigDecimal.ZERO
        )
    }

    private fun loadCategoryMonthsData(
        groupId: ObjectId,
        categoryId: ObjectId,
        months: Int,
        language: String?
    ): List<CategoryMonthData> {
        val now = LocalDate.now()
        val periodStart = now.minusMonths((months - 1).toLong()).withDayOfMonth(1)
        val periodEnd = now.withDayOfMonth(now.lengthOfMonth())
        val allOps = findOperations(groupId, periodStart, periodEnd)
            .filter { it.categoryId == categoryId }

        val opsByMonth = allOps.groupBy { YearMonth.from(it.operationDate) }

        return (months - 1 downTo 0).map { offset ->
            val yearMonth = YearMonth.from(now.minusMonths(offset.toLong()))
            val ops = opsByMonth[yearMonth] ?: emptyList()
            CategoryMonthData(
                label = "${monthNameShort(yearMonth.atDay(1), language)} ${yearMonth.year}",
                total = sumAmounts(ops),
                count = ops.size
            )
        }
    }

    private fun findMonthOperations(groupId: ObjectId, monthStart: LocalDate): List<FinanceOperationEntity> {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        return findOperations(groupId, monthStart, monthEnd)
    }

    private fun findOperations(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FinanceOperationEntity> =
        financeOperationRepository.findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
            groupId, startDate, endDate
        )

    private fun List<FinanceOperationEntity>.filterByType(type: CategoryType) =
        filter { it.type == type }

    private fun List<FinanceOperationEntity>.groupByCategory(): Map<Pair<String, String>, BigDecimal> =
        groupBy { (it.categoryIcon ?: DEFAULT_CATEGORY_ICON) to it.categoryName }
            .mapValues { (_, ops) -> sumAmounts(ops) }

    private fun sumAmounts(operations: List<FinanceOperationEntity>): BigDecimal =
        operations.fold(BigDecimal.ZERO) { acc, op -> acc.add(op.amount) }

    private fun formatYearRange(previousMonth: LocalDate, currentMonth: LocalDate): String =
        if (previousMonth.year == currentMonth.year) "${previousMonth.year}"
        else "${previousMonth.year}/${currentMonth.year}"

    private fun monthNameShort(date: LocalDate, language: String?): String =
        date.month.getDisplayName(TextStyle.SHORT_STANDALONE, localeFor(language))
            .replaceFirstChar { it.uppercaseChar() }

    private fun monthNameFull(date: LocalDate, language: String?): String =
        date.month.getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language))
            .replaceFirstChar { it.uppercaseChar() }

    private fun localeFor(language: String?): Locale =
        Locale.of(language ?: LocalizationService.FALLBACK_LANGUAGE)
}
