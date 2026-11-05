package ai.moneymanager.service.question

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.daysAfter
import ai.moneymanager.domain.model.question.AnswerBar
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.AnswerLink
import ai.moneymanager.domain.model.question.AnswerRow
import ai.moneymanager.domain.model.question.AnswerScreen
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.text.matchesEntityName
import ai.moneymanager.domain.text.matchesSearch
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.UserInfoService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val DEFAULT_TOP = 5
private const val MAX_ROWS = 10
private const val TREND_MONTHS = 2L
private const val TREND_THRESHOLD = 10
private const val DAYS_IN_WEEK = 7
private const val TAG_MARK = "#"
private const val DESCRIPTION_SEPARATOR = " · "
private const val MEMBER_ICON = "👤"

private data class Scope(
    val group: MoneyGroup,
    val categories: List<Category>,
    val members: Map<Long, String>,
    val today: LocalDate,
    val earliest: LocalDate?,
    val text: AnswerText,
)

private data class Selection(
    val type: CategoryType,
    val categories: List<Category>,
    val unknownCategories: List<String>,
    val member: Long?,
    val place: String?,
    val tag: String?,
)

private data class Slice(val range: QuestionRange, val operations: List<FinanceOperationEntity>, val limited: Boolean) {
    val total: BigDecimal
        get() = operations.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount }
}

@Service
class FinanceAnswerService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val categoryService: CategoryService,
    private val budgetService: BudgetService,
    private val recurringPaymentService: RecurringPaymentService,
    private val financeOperationService: FinanceOperationService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
) {
    fun answer(question: FinanceQuestion, group: MoneyGroup, language: String?, today: LocalDate, earliest: LocalDate?): AnswerCard {
        val scope = Scope(
            group = group,
            categories = categoryService.getCategoriesByGroup(checkNotNull(group.id)),
            members = userInfoService.displayNames(group.memberIds),
            today = today,
            earliest = earliest,
            text = AnswerText(localizationService, language, group.currency, today),
        )
        val selection = select(question, scope)
        if (selection.categories.isEmpty() && selection.unknownCategories.isNotEmpty()) return unknownCategory(selection, scope)
        return when (question.intent) {
            FinanceIntent.SUM, FinanceIntent.PLACE -> sum(question, selection, scope)
            FinanceIntent.COMPARE -> compare(question, selection, scope)
            FinanceIntent.BREAKDOWN -> breakdown(question, selection, scope)
            FinanceIntent.LARGEST -> largest(question, selection, scope)
            FinanceIntent.BUDGETS -> budgets(selection, scope)
            FinanceIntent.BALANCE -> balance(scope)
            FinanceIntent.RECURRING -> recurring(question, scope)
            FinanceIntent.MEMBERS -> if (selection.member != null) sum(question, selection, scope) else members(question, selection, scope)
            FinanceIntent.FORECAST -> forecast(scope)
            FinanceIntent.AVERAGE -> average(question, selection, scope)
            FinanceIntent.TRENDS -> trends(scope)
            FinanceIntent.AFFORDABILITY -> affordability(question, scope)
            FinanceIntent.SAVINGS -> savings(scope)
        }
    }

    fun notUnderstood(language: String?, suggestions: List<String>): AnswerCard =
        AnswerCard(
            title = localizationService.t("ask.unknown.title", language),
            lines = listOf(localizationService.t("ask.unknown.body", language)),
            followUps = suggestions,
            understood = false,
        )

    private fun sum(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val slice = slice(question, selection, scope)
        val title = subject(selection, scope) + DESCRIPTION_SEPARATOR + text.period(slice.range)
        if (slice.operations.isEmpty()) return AnswerCard(title, lines = limitedNote(slice, scope, listOf(text.t("empty"))), followUps = commonFollowUps(text), link = link(slice, selection))
        val lastDate = slice.operations.maxOf { it.operationDate }
        val lines = listOfNotNull(
            text.t("count", slice.operations.size),
            text.t("last", text.date(lastDate)).takeIf { selection.place != null || selection.tag != null },
        )
        return AnswerCard(title, text.money(slice.total), limitedNote(slice, scope, lines), followUps = commonFollowUps(text), link = link(slice, selection))
    }

    private fun compare(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val current = slice(question, selection, scope)
        val previousRange = question.comparePeriod?.let { resolvePeriod(it, scope.today, scope.group.monthStartDay) } ?: previousPeriod(current.range)
        val previous = slice(previousRange, selection, scope)
        val bars = listOf(
            AnswerBar(text.period(previous.range), previous.total, current = false),
            AnswerBar(text.period(current.range), current.total, current = true),
        )
        return AnswerCard(
            title = subject(selection, scope) + DESCRIPTION_SEPARATOR + text.period(current.range),
            headline = text.money(current.total),
            lines = limitedNote(previous, scope, listOf(text.change(current.total, previous.total))),
            bars = bars,
            followUps = listOf(text.t("follow.where"), text.t("follow.largest"), text.t("follow.trends")),
            link = link(current, selection),
        )
    }

    private fun breakdown(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val slice = slice(question, selection.copy(categories = emptyList()), scope)
        val rows = byCategory(slice.operations, scope)
            .take(question.limit ?: DEFAULT_TOP)
            .map { (category, total) -> AnswerRow(text.icon(category.icon), category.name, text.money(total), "${text.percent(total, slice.total)}%") }
        return AnswerCard(
            title = text.t("breakdown.title", text.period(slice.range)),
            headline = text.money(slice.total),
            lines = limitedNote(slice, scope, emptyList()),
            rows = rows,
            followUps = listOf(text.t("follow.previous"), text.t("follow.trends"), text.t("follow.largest")),
            link = link(slice, selection),
        )
    }

    private fun largest(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val slice = slice(question, selection, scope)
        val minimum = question.minAmount ?: BigDecimal.ZERO
        val rows = slice.operations.filter { it.amount >= minimum }
            .sortedByDescending { it.amount }
            .take(question.limit ?: DEFAULT_TOP)
            .map { AnswerRow(text.icon(it.categoryIcon), operationLabel(it), text.money(it.amount), text.date(it.operationDate)) }
        val busiest = slice.operations.groupBy { it.operationDate }.mapValues { (_, day) -> day.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount } }
            .maxByOrNull { it.value }
        val lines = listOfNotNull(busiest?.let { text.t("largest.day", text.date(it.key), text.money(it.value)) }).ifEmpty { listOf(text.t("empty")) }
        return AnswerCard(
            title = text.t("largest.title", text.period(slice.range)),
            headline = rows.firstOrNull()?.value,
            lines = limitedNote(slice, scope, lines),
            rows = rows,
            followUps = listOf(text.t("follow.where"), text.t("follow.previous")),
            link = link(slice, selection),
        )
    }

    private fun budgets(selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val ids = selection.categories.mapNotNull { it.id }.toSet()
        val budgets = budgetService.budgets(scope.group).filter { ids.isEmpty() || it.category.id in ids }
        if (budgets.isEmpty()) {
            return AnswerCard(text.t("budgets.title"), lines = listOf(text.t("budgets.none")), link = AnswerLink(AnswerScreen.CATEGORIES))
        }
        val rows = budgets.sortedByDescending { it.percent }.map { budget ->
            val status = if (budget.remaining.signum() >= 0) text.t("budgets.left", text.money(budget.remaining)) else text.t("budgets.over", text.money(budget.remaining.negate()))
            AnswerRow(text.icon(budget.category.icon), budget.category.name, status, "${budget.percent}%")
        }
        val fine = budgets.count { it.remaining.signum() >= 0 }
        val headline = budgets.singleOrNull()?.let { rows.single().value } ?: text.t("budgets.fine", fine, budgets.size)
        return AnswerCard(text.t("budgets.title"), headline, rows = rows, followUps = listOf(text.t("follow.forecast"), text.t("follow.savings")), link = AnswerLink(AnswerScreen.CATEGORIES))
    }

    private fun balance(scope: Scope): AnswerCard {
        val text = scope.text
        val total = financeOperationService.calculateBalance(checkNotNull(scope.group.id), scope.group.initialBalance).total
        val month = monthTotals(scope)
        val left = month.income - month.expense
        val days = daysAfter(scope.today, month.period) + 1
        val lines = listOfNotNull(
            text.t("balance.month", text.money(month.expense), text.money(month.income)),
            if (month.income.signum() > 0) text.t("balance.per_day", text.money(perDay(left, days)), days) else text.t("balance.no_income"),
        )
        return AnswerCard(text.t("balance.title"), text.money(total), lines, followUps = listOf(text.t("follow.forecast"), text.t("follow.where")), link = AnswerLink(AnswerScreen.REPORTS, month.period.month))
    }

    private fun recurring(question: FinanceQuestion, scope: Scope): AnswerCard {
        val text = scope.text
        val range = question.period?.let { resolvePeriod(it, scope.today, scope.group.monthStartDay) }
        val payments = recurringPaymentService.payments(checkNotNull(scope.group.id)).filter { it.active }
            .filter { range == null || range.contains(it.nextDate) }
        val title = range?.let { text.t("recurring.upcoming", text.period(it)) } ?: text.t("recurring.title")
        if (payments.isEmpty()) return AnswerCard(title, lines = listOf(text.t("recurring.none")), link = AnswerLink(AnswerScreen.RECURRING))
        val monthly = payments.filter { it.type == CategoryType.EXPENSE }.fold(BigDecimal.ZERO) { sum, payment -> sum + payment.recurrence.monthly(payment.amount) }
        val rows = payments.sortedBy { it.nextDate }.take(MAX_ROWS).map {
            AnswerRow(text.icon(it.categoryIcon), it.description ?: it.categoryName, text.money(it.amount), text.t("recurring.next", text.date(it.nextDate)))
        }
        return AnswerCard(title, text.t("recurring.monthly", text.money(monthly)), rows = rows, followUps = listOf(text.t("follow.where"), text.t("follow.balance")), link = AnswerLink(AnswerScreen.RECURRING))
    }

    private fun members(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val slice = slice(question, selection, scope)
        val rows = slice.operations.groupBy { it.creatorId }
            .mapValues { (_, operations) -> operations.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount } }
            .entries.sortedByDescending { it.value }
            .map { (memberId, total) -> AnswerRow(MEMBER_ICON, scope.members[memberId] ?: memberId.toString(), text.money(total), "${text.percent(total, slice.total)}%") }
        return AnswerCard(
            title = text.t("members.title", text.period(slice.range)),
            headline = text.money(slice.total),
            lines = limitedNote(slice, scope, emptyList()),
            rows = rows,
            followUps = listOf(text.t("follow.where"), text.t("follow.previous")),
            link = link(slice, selection),
        )
    }

    private fun forecast(scope: Scope): AnswerCard {
        val text = scope.text
        val month = monthTotals(scope)
        val elapsed = ChronoUnit.DAYS.between(month.period.start, scope.today).toInt() + 1
        val forecast = month.expense.multiply(BigDecimal(month.period.days)).divide(BigDecimal(elapsed), 0, RoundingMode.HALF_UP)
        val lines = listOfNotNull(
            text.t("forecast.spent", text.money(month.expense), elapsed),
            when {
                month.income.signum() <= 0 -> null
                forecast > month.income -> text.t("forecast.over", text.money(forecast - month.income))
                else -> text.t("forecast.fits", text.money(month.income - forecast))
            },
        )
        return AnswerCard(text.t("forecast.title", text.period(QuestionRange(month.period.start, month.period.end, month.period))), text.money(forecast), lines,
            followUps = listOf(text.t("follow.where"), text.t("follow.savings")), link = AnswerLink(AnswerScreen.REPORTS, month.period.month))
    }

    private fun average(question: FinanceQuestion, selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val slice = slice(question, selection, scope)
        val start = slice.range.from ?: slice.operations.minOfOrNull { it.operationDate } ?: scope.today
        val days = (ChronoUnit.DAYS.between(start, minOf(slice.range.to, scope.today)).toInt() + 1).coerceAtLeast(1)
        val perWeek = (slice.operations.size * DAYS_IN_WEEK + days / 2) / days
        return AnswerCard(
            title = subject(selection, scope) + DESCRIPTION_SEPARATOR + text.period(slice.range),
            headline = text.t("average.per_day", text.money(perDay(slice.total, days))),
            lines = limitedNote(slice, scope, listOf(text.t("average.count", slice.operations.size, perWeek))),
            followUps = listOf(text.t("follow.previous"), text.t("follow.trends")),
            link = link(slice, selection),
        )
    }

    private fun trends(scope: Scope): AnswerCard {
        val text = scope.text
        val rows = growth(scope).map { (category, current, change) -> AnswerRow(text.icon(category.icon), category.name, text.money(current), "+$change%") }
        val headline = rows.firstOrNull()?.let { "${it.icon} ${it.label} ${it.note}" }
        val lines = if (rows.isEmpty()) listOf(text.t("trends.none")) else listOf(text.t("trends.hint"))
        return AnswerCard(text.t("trends.title"), headline, limitedNote(Slice(QuestionRange(trendStart(scope), scope.today), emptyList(), scope.earliest != null && scope.earliest > trendStart(scope)), scope, lines),
            rows = rows, followUps = listOf(text.t("follow.savings"), text.t("follow.where")), link = AnswerLink(AnswerScreen.REPORTS))
    }

    private fun affordability(question: FinanceQuestion, scope: Scope): AnswerCard {
        val text = scope.text
        val price = question.amount ?: return AnswerCard(text.t("afford.title", ""), lines = listOf(text.t("afford.no_amount")), understood = false)
        val month = monthTotals(scope)
        val left = month.income - month.expense
        val after = left - price
        val days = daysAfter(scope.today, month.period).coerceAtLeast(1)
        val lines = listOfNotNull(
            text.t("afford.month_left", text.money(left)),
            text.t("afford.per_day", text.money(perDay(after, days)), days).takeIf { after.signum() > 0 },
            text.t("afford.no_income").takeIf { month.income.signum() <= 0 },
        )
        val headline = if (after.signum() >= 0) text.t("afford.left", text.money(after)) else text.t("afford.short", text.money(after.negate()))
        return AnswerCard(text.t("afford.title", text.money(price)), headline, lines, followUps = listOf(text.t("follow.forecast"), text.t("follow.savings")),
            link = AnswerLink(AnswerScreen.REPORTS, month.period.month))
    }

    private fun savings(scope: Scope): AnswerCard {
        val text = scope.text
        val over = budgetService.budgets(scope.group).filter { it.remaining.signum() < 0 }
            .map { AnswerRow(text.icon(it.category.icon), it.category.name, text.t("budgets.over", text.money(it.remaining.negate())), text.t("savings.over_budget")) }
        val growing = growth(scope).map { (category, current, change) -> AnswerRow(text.icon(category.icon), category.name, text.money(current), text.t("savings.growing", change)) }
        val month = monthTotals(scope)
        val biggest = byCategory(month.expenses, scope).firstOrNull()?.let { (category, total) ->
            AnswerRow(text.icon(category.icon), category.name, text.money(total), text.t("savings.biggest", text.percent(total, month.expense)))
        }
        val rows = (over + growing + listOfNotNull(biggest)).distinctBy { it.label }.take(DEFAULT_TOP)
        return AnswerCard(text.t("savings.title"), lines = listOf(if (rows.isEmpty()) text.t("savings.none") else text.t("savings.hint")), rows = rows,
            followUps = listOf(text.t("follow.trends"), text.t("follow.budgets")), link = AnswerLink(AnswerScreen.REPORTS, month.period.month))
    }

    private fun unknownCategory(selection: Selection, scope: Scope): AnswerCard {
        val text = scope.text
        val known = scope.categories.filter { it.type == selection.type }.take(DEFAULT_TOP)
        return AnswerCard(
            title = text.t("category.unknown", selection.unknownCategories.joinToString()),
            lines = listOf(text.t("category.known")),
            rows = known.map { AnswerRow(text.icon(it.icon), it.name, "") },
            followUps = known.take(3).map { text.t("follow.category", it.name) },
            link = AnswerLink(AnswerScreen.CATEGORIES),
        )
    }

    private fun select(question: FinanceQuestion, scope: Scope): Selection {
        val matched = question.categories.associateWith { name -> scope.categories.filter { matchesEntityName(it.name, name) } }
        val categories = matched.values.flatten().distinctBy { it.id }
        val type = question.type ?: categories.map { it.type }.distinct().singleOrNull() ?: CategoryType.EXPENSE
        val member = question.member?.trim()?.lowercase()?.let { name ->
            scope.members.entries.firstOrNull { (_, display) -> display.lowercase().contains(name) || name.contains(display.lowercase()) }?.key
        }
        return Selection(
            type = type,
            categories = categories.filter { it.type == type },
            unknownCategories = matched.filterValues { it.isEmpty() }.keys.toList(),
            member = member,
            place = question.place?.trim()?.takeIf { it.isNotEmpty() },
            tag = question.tag?.trim()?.removePrefix(TAG_MARK)?.takeIf { it.isNotEmpty() },
        )
    }

    private fun slice(question: FinanceQuestion, selection: Selection, scope: Scope): Slice =
        slice(resolvePeriod(question.period, scope.today, scope.group.monthStartDay, fallbackOf(question)), selection, scope)

    private fun slice(range: QuestionRange, selection: Selection, scope: Scope): Slice {
        val limited = scope.earliest != null && (range.from == null || range.from.isBefore(scope.earliest))
        val from = if (limited) scope.earliest else range.from
        val ids = selection.categories.mapNotNull { it.id }.toSet()
        val operations = financeOperationRepository.findAll(OperationFilter(checkNotNull(scope.group.id), from, range.to)).filter { operation ->
            operation.type == selection.type &&
                (ids.isEmpty() || operation.categoryId in ids) &&
                (selection.member == null || operation.creatorId == selection.member) &&
                (selection.place == null || matchesSearch(operation, selection.place)) &&
                (selection.tag == null || matchesSearch(operation, TAG_MARK + selection.tag))
        }
        return Slice(range, operations, limited)
    }

    private data class MonthTotals(val period: MonthPeriod, val income: BigDecimal, val expense: BigDecimal, val expenses: List<FinanceOperationEntity>)

    private fun monthTotals(scope: Scope): MonthTotals {
        val period = currentMonthPeriod(scope.today, scope.group.monthStartDay)
        val operations = financeOperationRepository.findAll(OperationFilter(checkNotNull(scope.group.id), period.start, period.end))
        val expenses = operations.filter { it.type == CategoryType.EXPENSE }
        val income = operations.filter { it.type == CategoryType.INCOME }.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount }
        return MonthTotals(period, income, expenses.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount }, expenses)
    }

    private fun growth(scope: Scope): List<Triple<Category, BigDecimal, Int>> {
        val current = currentMonthPeriod(scope.today, scope.group.monthStartDay)
        val start = trendStart(scope)
        val before = financeOperationRepository.findAll(OperationFilter(checkNotNull(scope.group.id), maxOf(start, scope.earliest ?: start), current.start.minusDays(1)))
            .filter { it.type == CategoryType.EXPENSE }
        val now = monthTotals(scope).expenses
        val previousAverage = before.groupBy { it.categoryId }.mapValues { (_, operations) -> operations.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount }.divide(BigDecimal(TREND_MONTHS), 0, RoundingMode.HALF_UP) }
        return byCategory(now, scope).mapNotNull { (category, total) ->
            val average = previousAverage[category.id] ?: return@mapNotNull null
            if (average.signum() <= 0) return@mapNotNull null
            val change = scope.text.percent(total - average, average)
            Triple(category, total, change).takeIf { change >= TREND_THRESHOLD }
        }.sortedByDescending { it.third }.take(DEFAULT_TOP)
    }

    private fun trendStart(scope: Scope): LocalDate = currentMonthPeriod(scope.today, scope.group.monthStartDay).start.minusMonths(TREND_MONTHS)

    private fun byCategory(operations: List<FinanceOperationEntity>, scope: Scope): List<Pair<Category, BigDecimal>> {
        val byId = scope.categories.associateBy { it.id }
        return operations.groupBy { it.categoryId }.map { (categoryId, group) ->
            val first = group.first()
            val category = byId[categoryId] ?: Category(categoryId, first.categoryName, first.categoryIcon, first.type, checkNotNull(scope.group.id))
            category to group.fold(BigDecimal.ZERO) { sum, operation -> sum + operation.amount }
        }.sortedByDescending { it.second }
    }

    private fun subject(selection: Selection, scope: Scope): String {
        val text = scope.text
        val base = when {
            selection.categories.isNotEmpty() -> text.categories(selection.categories)
            selection.place != null -> text.t("subject.place", selection.place)
            selection.tag != null -> TAG_MARK + selection.tag
            selection.type == CategoryType.INCOME -> text.t("subject.income")
            else -> text.t("subject.expense")
        }
        return selection.member?.let { base + DESCRIPTION_SEPARATOR + (scope.members[it] ?: it.toString()) } ?: base
    }

    private fun limitedNote(slice: Slice, scope: Scope, lines: List<String>): List<String> =
        if (slice.limited && scope.earliest != null) lines + scope.text.t("premium_limited", scope.text.date(scope.earliest)) else lines

    private fun link(slice: Slice, selection: Selection): AnswerLink {
        val search = selection.place ?: selection.tag?.let { TAG_MARK + it }
        return if (search != null || slice.range.month == null) {
            AnswerLink(AnswerScreen.OPERATIONS, search = search)
        } else {
            AnswerLink(AnswerScreen.REPORTS, YearMonth.from(slice.range.month.start))
        }
    }

    private fun commonFollowUps(text: AnswerText): List<String> = listOf(text.t("follow.previous"), text.t("follow.largest"), text.t("follow.where"))

    private fun operationLabel(operation: FinanceOperationEntity): String =
        operation.categoryName + (operation.description?.let { DESCRIPTION_SEPARATOR + it } ?: "")

    private fun perDay(amount: BigDecimal, days: Int): BigDecimal = amount.divide(BigDecimal(days.coerceAtLeast(1)), 0, RoundingMode.DOWN)

    private fun fallbackOf(question: FinanceQuestion): PeriodPreset =
        if (question.intent == FinanceIntent.AVERAGE) PeriodPreset.LAST_3_MONTHS else PeriodPreset.THIS_MONTH
}
