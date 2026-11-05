package ai.moneymanager.service.question

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.model.question.PeriodSpec
import ai.moneymanager.testing.TEST_USER_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth

private class MovingClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
}

class QuestionMemoryTest {
    private val clock = MovingClock(Instant.parse("2026-11-20T10:00:00Z"))
    private val memory = QuestionMemory(clock)
    private val question = FinanceQuestion(FinanceIntent.SUM, CategoryType.EXPENSE, listOf("Кафе"), PeriodSpec.Month(YearMonth.of(2026, 10)))

    @Test
    fun `keeps the last question for a short follow-up`() {
        memory.remember(TEST_USER_ID, question)
        clock.now = clock.now.plus(Duration.ofMinutes(4))

        assertThat(memory.recall(TEST_USER_ID)).isEqualTo(question)
    }

    @Test
    fun `forgets the question once the follow-up window passes`() {
        memory.remember(TEST_USER_ID, question)
        clock.now = clock.now.plus(Duration.ofMinutes(6))

        assertThat(memory.recall(TEST_USER_ID)).isNull()
    }

    @Test
    fun `describes the question as the call gemini would make`() {
        val compare = question.copy(intent = FinanceIntent.COMPARE, comparePeriod = PeriodSpec.Preset(PeriodPreset.LAST_MONTH), member = "Аня")

        assertThat(askFinanceCallOf(compare)).isEqualTo(
            """askFinance(intent=COMPARE, operationType=EXPENSE, month=2026-10, comparePeriod=LAST_MONTH, categories="Кафе", member="Аня")"""
        )
    }
}
