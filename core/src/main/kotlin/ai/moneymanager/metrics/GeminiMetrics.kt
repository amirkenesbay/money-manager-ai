package ai.moneymanager.metrics

import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.enum.AiSurface
import com.google.genai.errors.ClientException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

const val GEMINI_QUOTA_STATUS = 429

private const val GEMINI_REQUESTS = "moneymanager.gemini.requests"
private const val CALL_TAG = "call"
private const val OUTCOME_TAG = "outcome"
private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_QUOTA = "quota"
private const val OUTCOME_ERROR = "error"

private const val AI_RESULTS = "moneymanager.ai.results"
private const val SURFACE_TAG = "surface"
private const val RESULT_TAG = "result"

enum class AiResult(val tag: String) {
    OPERATION("operation"),
    QUESTION("question"),
    NOT_UNDERSTOOD("not_understood"),
    INVALID("invalid"),
    UNAVAILABLE("unavailable"),
    OTHER("other"),
}

enum class GeminiCall(val tag: String) {
    TEXT("text"),
    COMMAND("command"),
    VOICE("voice"),
}

private val OUTCOMES = listOf(OUTCOME_SUCCESS, OUTCOME_QUOTA, OUTCOME_ERROR)

@Component
class GeminiMetrics(private val registry: MeterRegistry) {
    init {
        GeminiCall.entries.forEach { call -> OUTCOMES.forEach { outcome -> timer(call, outcome) } }
        AiSurface.entries.forEach { surface -> AiResult.entries.forEach { result -> counter(surface, result) } }
    }

    fun recordResults(surface: AiSurface, commands: List<BotCommand>) {
        commands.forEach { counter(surface, resultOf(it)).increment() }
    }

    private fun resultOf(command: BotCommand): AiResult = when (command) {
        is BotCommand.AddExpense, is BotCommand.AddIncome, is BotCommand.AddLoan, is BotCommand.RepayLoan -> AiResult.OPERATION
        is BotCommand.AskFinance -> AiResult.QUESTION
        is BotCommand.OutOfContext -> AiResult.NOT_UNDERSTOOD
        is BotCommand.ParseError -> AiResult.INVALID
        is BotCommand.ServiceError, is BotCommand.RateLimitError -> AiResult.UNAVAILABLE
        else -> AiResult.OTHER
    }

    private fun counter(surface: AiSurface, result: AiResult): Counter =
        Counter.builder(AI_RESULTS).tag(SURFACE_TAG, surface.name.lowercase()).tag(RESULT_TAG, result.tag).register(registry)

    fun <T> timed(call: GeminiCall, block: () -> T): T {
        val sample = Timer.start(registry)
        var outcome = OUTCOME_SUCCESS
        try {
            return block()
        } catch (failure: Exception) {
            outcome = if (failure is ClientException && failure.code() == GEMINI_QUOTA_STATUS) OUTCOME_QUOTA else OUTCOME_ERROR
            throw failure
        } finally {
            sample.stop(timer(call, outcome))
        }
    }

    private fun timer(call: GeminiCall, outcome: String): Timer =
        Timer.builder(GEMINI_REQUESTS).tag(CALL_TAG, call.tag).tag(OUTCOME_TAG, outcome).register(registry)
}
