package ai.moneymanager.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

private const val CALENDAR_START = 1
private const val SALARY_DAY = 10

class DigestScheduleTest {
    @Test
    fun `sends the day summary from nine in the evening`() {
        val today = LocalDate.of(2026, 11, 4)

        assertThat(dueDigestPeriod(DigestKind.DAY, today.atTime(20, 59), CALENDAR_START)).isNull()
        assertThat(dueDigestPeriod(DigestKind.DAY, today.atTime(21, 0), CALENDAR_START)).isEqualTo(DigestPeriod(DigestKind.DAY, today, today))
    }

    @Test
    fun `sends the week summary on sunday evening and catches up for two days`() {
        val week = DigestPeriod(DigestKind.WEEK, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8))

        assertThat(dueDigestPeriod(DigestKind.WEEK, LocalDateTime.of(2026, 11, 8, 19, 59), CALENDAR_START)).isNull()
        assertThat(dueDigestPeriod(DigestKind.WEEK, LocalDateTime.of(2026, 11, 8, 20, 0), CALENDAR_START)).isEqualTo(week)
        assertThat(dueDigestPeriod(DigestKind.WEEK, LocalDateTime.of(2026, 11, 10, 19, 59), CALENDAR_START)).isEqualTo(week)
        assertThat(dueDigestPeriod(DigestKind.WEEK, LocalDateTime.of(2026, 11, 10, 20, 0), CALENDAR_START)).isNull()
    }

    @Test
    fun `sends the closed month on the morning the new one starts`() {
        assertThat(dueDigestPeriod(DigestKind.MONTH, LocalDateTime.of(2026, 12, 1, 9, 59), CALENDAR_START)).isNull()
        assertThat(dueDigestPeriod(DigestKind.MONTH, LocalDateTime.of(2026, 12, 1, 10, 0), CALENDAR_START))
            .isEqualTo(DigestPeriod(DigestKind.MONTH, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
        assertThat(dueDigestPeriod(DigestKind.MONTH, LocalDateTime.of(2026, 12, 3, 10, 0), CALENDAR_START)).isNull()
    }

    @Test
    fun `follows the group month start day`() {
        assertThat(dueDigestPeriod(DigestKind.MONTH, LocalDateTime.of(2026, 12, 10, 10, 30), SALARY_DAY))
            .isEqualTo(DigestPeriod(DigestKind.MONTH, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 12, 9)))
    }
}
