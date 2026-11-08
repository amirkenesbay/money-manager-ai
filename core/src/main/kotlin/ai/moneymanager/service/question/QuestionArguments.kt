package ai.moneymanager.service.question

import ai.moneymanager.domain.text.geminiValue
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.arguments.AskFinanceArgs
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.model.question.PeriodSpec
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeParseException

private const val LIST_SEPARATOR = ","
private const val CALL_TEMPLATE = "askFinance(%s)"
private const val ARGUMENT_TEMPLATE = "%s=%s"
private const val QUOTED_TEMPLATE = "\"%s\""
private const val ARGUMENTS_SEPARATOR = ", "

fun financeQuestionOf(args: AskFinanceArgs): FinanceQuestion? {
    val intent = FinanceIntent.of(args.intent) ?: return null
    return FinanceQuestion(
        intent = intent,
        type = categoryTypeOf(args.operationType),
        categories = args.categories?.split(LIST_SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        period = periodOf(args.period, args.month, args.from, args.to),
        comparePeriod = periodOf(args.comparePeriod, args.compareMonth, null, null),
        member = args.member.geminiValue(),
        place = args.place.geminiValue(),
        tag = args.tag.geminiValue(),
        minAmount = args.minAmount?.takeIf { it > 0 }?.let { BigDecimal.valueOf(it) },
        limit = args.limit?.toInt()?.takeIf { it > 0 },
        amount = args.amount?.takeIf { it > 0 }?.let { BigDecimal.valueOf(it) },
    )
}

fun financeQuestionOf(command: BotCommand, today: LocalDate): FinanceQuestion? = when (command) {
    is BotCommand.AskFinance -> command.question
    is BotCommand.ShowBalance -> FinanceQuestion(FinanceIntent.BALANCE)
    is BotCommand.ShowReport -> FinanceQuestion(FinanceIntent.BREAKDOWN, period = reportPeriodOf(command, today))
    is BotCommand.ShowHistory -> FinanceQuestion(
        intent = FinanceIntent.BREAKDOWN,
        type = categoryTypeOf(command.type),
        categories = listOfNotNull(command.categoryFilter.geminiValue()),
        period = periodOf(null, null, command.startDate, command.endDate),
    )
    else -> null
}

private fun categoryTypeOf(name: String?): CategoryType? =
    CategoryType.entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }

private fun reportPeriodOf(command: BotCommand.ShowReport, today: LocalDate): PeriodSpec? =
    command.month?.takeIf { it in Month.JANUARY.value..Month.DECEMBER.value }
        ?.let { PeriodSpec.Month(YearMonth.of(command.year ?: today.year, it)) }

private fun periodOf(preset: String?, month: String?, from: String?, to: String?): PeriodSpec? {
    month.parsedOrNull { YearMonth.parse(it) }?.let { return PeriodSpec.Month(it) }
    val start = from.parsedOrNull { LocalDate.parse(it) }
    val end = to.parsedOrNull { LocalDate.parse(it) }
    if (start != null || end != null) return PeriodSpec.Range(start ?: checkNotNull(end), end ?: start ?: checkNotNull(end))
    return PeriodPreset.of(preset)?.let { PeriodSpec.Preset(it) }
}

private fun <T> String?.parsedOrNull(parse: (String) -> T): T? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let {
        try {
            parse(it)
        } catch (exception: DateTimeParseException) {
            null
        }
    }


fun askFinanceCallOf(question: FinanceQuestion): String {
    val arguments = listOf("intent" to question.intent.name, "operationType" to question.type?.name) +
        periodArguments(question.period, "period", "month", "from", "to") +
        periodArguments(question.comparePeriod, "comparePeriod", "compareMonth", null, null) +
        listOf(
            "categories" to question.categories.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEPARATOR)?.let(::quoted),
            "member" to question.member?.let(::quoted),
            "place" to question.place?.let(::quoted),
            "tag" to question.tag?.let(::quoted),
            "minAmount" to question.minAmount?.toPlainString(),
            "limit" to question.limit?.toString(),
            "amount" to question.amount?.toPlainString(),
        )
    val rendered = arguments.mapNotNull { (name, value) -> value?.let { ARGUMENT_TEMPLATE.format(name, it) } }
    return CALL_TEMPLATE.format(rendered.joinToString(ARGUMENTS_SEPARATOR))
}

private fun periodArguments(period: PeriodSpec?, presetName: String, monthName: String, fromName: String?, toName: String?): List<Pair<String, String?>> =
    when (period) {
        null -> emptyList()
        is PeriodSpec.Preset -> listOf(presetName to period.preset.name)
        is PeriodSpec.Month -> listOf(monthName to period.month.toString())
        is PeriodSpec.Range -> listOfNotNull(fromName?.let { it to period.from.toString() }, toName?.let { it to period.to.toString() })
    }

private fun quoted(value: String): String = QUOTED_TEMPLATE.format(value)
