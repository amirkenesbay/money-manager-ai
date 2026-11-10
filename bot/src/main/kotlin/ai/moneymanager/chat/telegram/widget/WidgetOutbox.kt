package ai.moneymanager.chat.telegram.widget

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val CARD_LIFETIME: Duration = Duration.ofSeconds(30)

@Component
class WidgetOutbox(private val clock: Clock) {
    private data class Pending(val cards: List<OperationCard>, val queuedAt: Instant)

    private val pending = ConcurrentHashMap<String, Pending>()

    fun add(chatId: Long, card: OperationCard) {
        val now = clock.instant()
        pending.compute(chatId.toString()) { _, current ->
            val cards = current?.takeIf { it.fresh(now) }?.cards.orEmpty()
            Pending(cards + card, now)
        }
    }

    fun take(chatId: String, text: String?): List<OperationCard>? {
        val current = pending.remove(chatId)?.takeIf { it.fresh(clock.instant()) } ?: return null
        return current.cards.takeIf { cards -> text != null && cards.all { text.contains(it.fallbackText) } }
    }

    private fun Pending.fresh(now: Instant): Boolean = Duration.between(queuedAt, now) < CARD_LIFETIME
}
