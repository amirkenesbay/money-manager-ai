package ai.moneymanager.metrics

import ai.moneymanager.domain.event.BotMembershipChanged
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

private const val USER_ID = 42L

class MessageMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = MessageMetrics(registry)

    private fun count(type: MessageType, outcome: MessageOutcome) =
        registry.counter("moneymanager.messages", "type", type.tag, "outcome", outcome.tag).count()

    private fun blockedAfter(type: MessageType) = registry.counter("moneymanager.messages.blocked_after", "type", type.tag).count()

    @Test
    fun `counts sent and failed messages by type`() {
        metrics.track(MessageType.DIGEST, USER_ID) { }
        assertThatThrownBy { metrics.track(MessageType.DIGEST, USER_ID) { error("telegram is down") } }

        assertThat(count(MessageType.DIGEST, MessageOutcome.SENT)).isEqualTo(1.0)
        assertThat(count(MessageType.DIGEST, MessageOutcome.FAILED)).isEqualTo(1.0)
    }

    @Test
    fun `blames the last message when the user blocks the bot right after it`() {
        metrics.track(MessageType.SPENDING_REMINDER, USER_ID) { }

        metrics.onBotMembership(BotMembershipChanged(USER_ID, blocked = true))

        assertThat(blockedAfter(MessageType.SPENDING_REMINDER)).isEqualTo(1.0)
    }

    @Test
    fun `does not blame anything when the user had no messages`() {
        metrics.onBotMembership(BotMembershipChanged(USER_ID, blocked = true))

        assertThat(MessageType.entries.sumOf { blockedAfter(it) }).isEqualTo(0.0)
    }
}
