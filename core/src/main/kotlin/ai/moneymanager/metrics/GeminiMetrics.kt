package ai.moneymanager.metrics

import com.google.genai.errors.ClientException
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

enum class GeminiCall(val tag: String) {
    TEXT("text"),
    COMMAND("command"),
    VOICE("voice"),
}

@Component
class GeminiMetrics(private val registry: MeterRegistry) {
    fun <T> timed(call: GeminiCall, block: () -> T): T {
        val sample = Timer.start(registry)
        var outcome = OUTCOME_SUCCESS
        try {
            return block()
        } catch (failure: Exception) {
            outcome = if (failure is ClientException && failure.code() == GEMINI_QUOTA_STATUS) OUTCOME_QUOTA else OUTCOME_ERROR
            throw failure
        } finally {
            sample.stop(Timer.builder(GEMINI_REQUESTS).tag(CALL_TAG, call.tag).tag(OUTCOME_TAG, outcome).register(registry))
        }
    }
}
