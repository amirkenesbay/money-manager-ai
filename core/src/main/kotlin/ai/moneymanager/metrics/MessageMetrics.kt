package ai.moneymanager.metrics

import ai.moneymanager.domain.event.BotMembershipChanged
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val MESSAGES = "moneymanager.messages"
private const val BLOCKED_AFTER = "moneymanager.messages.blocked_after"
private const val TYPE_TAG = "type"
private const val OUTCOME_TAG = "outcome"
private val BLOCK_WINDOW: Duration = Duration.ofHours(24)

enum class MessageType(val tag: String) {
    SPENDING_REMINDER("spending_reminder"),
    REMINDER("reminder"),
    DIGEST("digest"),
    RECURRING("recurring"),
    LOAN("loan");

    companion object {
        fun of(tag: String?): MessageType? = entries.firstOrNull { it.tag == tag }
    }
}

enum class MessageOutcome(val tag: String) {
    SENT("sent"),
    FAILED("failed"),
    SKIPPED("skipped"),
    OPENED("opened"),
}

private data class SentMessage(val type: MessageType, val at: Instant)

@Component
class MessageMetrics(private val registry: MeterRegistry) {
    private val lastSent = ConcurrentHashMap<Long, SentMessage>()

    init {
        MessageType.entries.forEach { type ->
            MessageOutcome.entries.forEach { outcome -> counter(type, outcome) }
            blockedAfter(type)
        }
    }

    fun <T> track(type: MessageType, telegramUserId: Long, send: () -> T): T {
        val result = try {
            send()
        } catch (failure: Exception) {
            counter(type, MessageOutcome.FAILED).increment()
            throw failure
        }
        counter(type, MessageOutcome.SENT).increment()
        lastSent[telegramUserId] = SentMessage(type, Instant.now())
        return result
    }

    fun skipped(type: MessageType) = counter(type, MessageOutcome.SKIPPED).increment()

    fun opened(type: MessageType) = counter(type, MessageOutcome.OPENED).increment()

    @EventListener
    fun onBotMembership(event: BotMembershipChanged) {
        if (!event.blocked) return
        val last = lastSent.remove(event.telegramUserId) ?: return
        if (Duration.between(last.at, Instant.now()) <= BLOCK_WINDOW) blockedAfter(last.type).increment()
    }

    private fun counter(type: MessageType, outcome: MessageOutcome): Counter =
        registry.counter(MESSAGES, TYPE_TAG, type.tag, OUTCOME_TAG, outcome.tag)

    private fun blockedAfter(type: MessageType): Counter = registry.counter(BLOCKED_AFTER, TYPE_TAG, type.tag)
}
