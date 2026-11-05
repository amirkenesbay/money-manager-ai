package ai.moneymanager.service

import ai.moneymanager.testing.TEST_FREE_HISTORY_DAYS
import ai.moneymanager.testing.testSubscriptionLimits
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val FREE_HISTORY_DAYS = TEST_FREE_HISTORY_DAYS
private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)

class SubscriptionLimitsServiceTest {
    private val limits = testSubscriptionLimits(TODAY)
    private val earliestFreeDate = TODAY.minusDays(FREE_HISTORY_DAYS.toLong())

    @Test
    fun `free history starts exactly the configured number of days back`() {
        assertThat(limits.earliestHistoryDate(hasPaidSubscription = false)).isEqualTo(earliestFreeDate)
    }

    @Test
    fun `paid history has no start`() {
        assertThat(limits.earliestHistoryDate(hasPaidSubscription = true)).isNull()
    }

    @Test
    fun `the earliest free day is still allowed, the day before is not`() {
        assertThat(limits.isBeyondHistoryLimit(earliestFreeDate, hasPaidSubscription = false)).isFalse()
        assertThat(limits.isBeyondHistoryLimit(earliestFreeDate.minusDays(1), hasPaidSubscription = false)).isTrue()
    }

    @Test
    fun `paid users are never beyond the history limit`() {
        assertThat(limits.isBeyondHistoryLimit(LocalDate.of(2000, 1, 1), hasPaidSubscription = true)).isFalse()
    }
}
