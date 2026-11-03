package ai.moneymanager.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val FREE_HISTORY_DAYS = 30
private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)

class SubscriptionLimitsServiceTest {
    private val limits = SubscriptionLimitsService(
        freeMaxAiRequestsPerDay = 10,
        paidMaxAiRequestsPerDay = 100,
        freeMaxCategoriesPerType = 10,
        paidMaxCategoriesPerType = 1000,
        freeMaxHistoryDaysBack = FREE_HISTORY_DAYS,
        freeMaxOwnedSharedGroups = 3,
        freeMaxActiveNotifications = 3,
        freeMaxBudgets = 3,
        clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
    )
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
