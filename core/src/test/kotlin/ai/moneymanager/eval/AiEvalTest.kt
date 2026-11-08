package ai.moneymanager.eval

import ai.moneymanager.config.GeminiProperties
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.enum.AiSurface
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.model.question.PeriodSpec
import ai.moneymanager.mapper.GeminiArgsMapper
import ai.moneymanager.metrics.GeminiMetrics
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.nlp.WAV_MIME_TYPE
import ai.moneymanager.service.nlp.repeatOf
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.bson.types.ObjectId
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

private const val CASES = "/ai-eval/cases.json"
private const val LAST_WEEKDAY_PREFIX = "last-"
private const val DEFAULT_LANGUAGE = "ru"
private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
private const val AMOUNT_TOLERANCE = 0.01
private const val REPORT_DIR = "build/ai-eval"
private const val AUDIO_DIR = "build/ai-eval/audio"
private const val SAY_COMMAND = "say"
private const val CONVERT_COMMAND = "afconvert"
private const val WAV_FORMAT = "LEI16@16000"

private val EXPENSES = listOf("🛒" to "Продукты", "🚗" to "Транспорт", "🎬" to "Развлечения", "💊" to "Здоровье", "👕" to "Одежда", "📚" to "Образование", "📦" to "Другое")
private val INCOMES = listOf("💰" to "Зарплата", "🎁" to "Подарки", "📦" to "Другое")

data class EvalCase(val id: String, val group: String, val text: String, val expect: List<Map<String, Any?>>, val voice: String? = null, val language: String = DEFAULT_LANGUAGE)

data class EvalResult(val id: String, val group: String, val text: String, val passed: Boolean, val millis: Long, val actual: List<Map<String, Any?>>, val problems: List<String>)

@Tag("ai-eval")
class AiEvalTest {
    private val mapper = jacksonObjectMapper()
    private val model = System.getProperty("eval.model") ?: DEFAULT_MODEL
    private val label = System.getProperty("eval.label") ?: model
    private val only = System.getProperty("eval.only")?.split(",")?.toSet()
    private val delayMs = System.getProperty("eval.delayMs")?.toLong() ?: 0
    private val surface = System.getProperty("eval.surface")?.let { AiSurface.valueOf(it) } ?: AiSurface.MINI_APP
    private val groupId = ObjectId()
    private val categories = EXPENSES.map { (icon, name) -> Category(name = name, icon = icon, type = CategoryType.EXPENSE, groupId = groupId) } +
        INCOMES.map { (icon, name) -> Category(name = name, icon = icon, type = CategoryType.INCOME, groupId = groupId) }

    @Test
    fun evaluate() {
        val apiKey = checkNotNull(System.getenv("GEMINI_API_KEY")) { "GEMINI_API_KEY is not set" }
        val prompts = AiPromptService()
        val parser = CommandParserService(GeminiProperties(apiKey, model), GeminiArgsMapper(jacksonObjectMapper()), prompts, GeminiMetrics(SimpleMeterRegistry()))
        parser.init()
        val today = LocalDate.now()
        val cases = mapper.readValue<List<EvalCase>>(checkNotNull(javaClass.getResource(CASES)).readText())
            .filter { only == null || it.group in only || it.id in only }
        val results = cases.map { case ->
            val started = System.currentTimeMillis()
            val context = prompts.groupContext(today, Currency.KZT, categories, language = case.language)
            val commands = case.voice?.let { parser.parseVoiceCommands(speech(case, it), context, WAV_MIME_TYPE, surface) }
                ?: parser.parseCommands(case.text, context, surface)
            val actual = commands.map { normalize(it, today) }
            val millis = System.currentTimeMillis() - started
            if (delayMs > 0) Thread.sleep(delayMs)
            val problems = compare(case.expect, actual, today)
            EvalResult(case.id, case.group, case.text, problems.isEmpty(), millis, actual, problems)
        }
        report(results)
    }

    private fun normalize(command: BotCommand, today: LocalDate): Map<String, Any?> = when (command) {
        is BotCommand.AddExpense -> operation("addExpense", command.amount, command.category, command.operationDate, repeatOf(command)?.unit?.name, today)
        is BotCommand.AddIncome -> operation("addIncome", command.amount, command.category, command.operationDate, repeatOf(command)?.unit?.name, today)
        is BotCommand.AskFinance -> command.question.let { q ->
            mapOf(
                "fn" to "askFinance", "intent" to q.intent.name, "type" to q.type?.name, "categories" to q.categories,
                "period" to periodToken(q.period, today), "limit" to q.limit, "amount" to q.amount?.toDouble(),
            )
        }
        is BotCommand.AddLoan -> mapOf("fn" to "addLoan", "direction" to command.direction.name, "person" to command.person, "amount" to command.amount.toDouble())
        is BotCommand.RepayLoan -> mapOf("fn" to "repayLoan", "direction" to command.direction.name, "person" to command.person, "amount" to command.amount)
        is BotCommand.OutOfContext -> mapOf("fn" to "outOfContext")
        is BotCommand.ParseError -> mapOf("fn" to "ParseError", "error" to command.error)
        else -> mapOf("fn" to command::class.simpleName)
    }

    private fun operation(fn: String, amount: Double, category: String?, date: String?, repeatUnit: String?, today: LocalDate) = mapOf(
        "fn" to fn, "amount" to amount, "category" to category, "date" to (date ?: today.toString()), "repeatUnit" to repeatUnit,
    )

    private fun periodToken(period: PeriodSpec?, today: LocalDate): String? = when (period) {
        null -> null
        is PeriodSpec.Preset -> period.preset.name
        is PeriodSpec.Month -> if (period.month == YearMonth.from(today)) PeriodPreset.THIS_MONTH.name else "MONTH:%02d".format(period.month.monthValue)
        is PeriodSpec.Range -> "RANGE"
    }

    private fun speech(case: EvalCase, voice: String): ByteArray {
        val dir = File(AUDIO_DIR).apply { mkdirs() }
        val aiff = File(dir, "${case.id}.aiff")
        val wav = File(dir, "${case.id}.wav")
        run(SAY_COMMAND, "-v", voice, "-o", aiff.path, case.text)
        run(CONVERT_COMMAND, "-f", "WAVE", "-d", WAV_FORMAT, "-c", "1", aiff.path, wav.path)
        return wav.readBytes()
    }

    private fun run(vararg command: String) {
        val exit = ProcessBuilder(*command).inheritIO().start().waitFor()
        check(exit == 0) { "${command.first()} failed with $exit" }
    }

    private fun expectedDate(token: String, today: LocalDate): String = when {
        token == "today" -> today
        token == "yesterday" -> today.minusDays(1)
        token.startsWith(LAST_WEEKDAY_PREFIX) ->
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(token.removePrefix(LAST_WEEKDAY_PREFIX).uppercase())))
        else -> if (token.endsWith("d")) today.plusDays(token.removeSuffix("d").toLong()) else LocalDate.parse(token)
    }.toString()

    private fun compare(expected: List<Map<String, Any?>>, actual: List<Map<String, Any?>>, today: LocalDate): List<String> {
        if (expected.size != actual.size) return listOf("expected ${expected.size} calls, got ${actual.size}")
        return expected.zip(actual).flatMapIndexed { index, (want, got) ->
            want.mapNotNull { (key, value) ->
                val gotValue = got[key]
                val wanted = if (key == "date" && value is String) expectedDate(value, today) else value
                if (matches(wanted, gotValue)) null else "#${index + 1} $key: want $wanted, got $gotValue"
            }
        }
    }

    private fun matches(want: Any?, got: Any?): Boolean = when {
        want == null -> got == null
        got == null -> false
        want is Number && got is Number -> abs(want.toDouble() - got.toDouble()) < AMOUNT_TOLERANCE
        want is List<*> && got is List<*> -> want.map { it.toString().lowercase() }.toSet() == got.map { it.toString().lowercase() }.toSet()
        else -> want.toString().equals(got.toString(), ignoreCase = true)
    }

    private fun report(results: List<EvalResult>) {
        val lines = buildList {
            add("=== AI EVAL · $label · ${results.count { it.passed }}/${results.size} passed · avg ${results.map { it.millis }.average().toLong()} ms")
            results.groupBy { it.group }.forEach { (group, items) -> add("  %-8s %d/%d".format(group, items.count { it.passed }, items.size)) }
            results.filterNot { it.passed }.forEach { add("  ✗ ${it.id} «${it.text}» → ${it.problems.joinToString("; ")} | got ${it.actual}") }
        }
        lines.forEach(::println)
        File(REPORT_DIR).mkdirs()
        File("$REPORT_DIR/$label.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results))
        File("$REPORT_DIR/$label.txt").writeText(lines.joinToString("\n"))
    }
}
