package ai.moneymanager.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

private const val SALARY_DAY = 25
private const val LAST_ALLOWED_DAY = 28

class MonthPeriodTest {
    @Test
    fun `keeps the calendar month when the month starts on the first`() {
        val period = monthPeriod(YearMonth.of(2026, 2), DEFAULT_MONTH_START_DAY)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(period.days).isEqualTo(28)
    }

    @Test
    fun `runs from the start day to the day before it next month`() {
        val period = monthPeriod(YearMonth.of(2026, 10), SALARY_DAY)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 10, 25))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 11, 24))
        assertThat(period.days).isEqualTo(31)
    }

    @Test
    fun `crosses the new year`() {
        val period = monthPeriod(YearMonth.of(2026, 12), SALARY_DAY)

        assertThat(period.end).isEqualTo(LocalDate.of(2027, 1, 24))
    }

    @Test
    fun `fits february with the latest allowed start day`() {
        val period = monthPeriod(YearMonth.of(2027, 1), LAST_ALLOWED_DAY)

        assertThat(period.end).isEqualTo(LocalDate.of(2027, 2, 27))
    }

    @Test
    fun `belongs to the previous month before the start day`() {
        assertThat(currentMonthPeriod(LocalDate.of(2026, 10, 24), SALARY_DAY).month).isEqualTo(YearMonth.of(2026, 9))
        assertThat(currentMonthPeriod(LocalDate.of(2026, 10, 25), SALARY_DAY).month).isEqualTo(YearMonth.of(2026, 10))
        assertThat(currentMonthPeriod(LocalDate.of(2027, 1, 3), SALARY_DAY).month).isEqualTo(YearMonth.of(2026, 12))
    }

    @Test
    fun `restores the date of a day of month inside the period`() {
        val period = monthPeriod(YearMonth.of(2026, 10), SALARY_DAY)

        assertThat(period.dateOf(27)).isEqualTo(LocalDate.of(2026, 10, 27))
        assertThat(period.dateOf(3)).isEqualTo(LocalDate.of(2026, 11, 3))
    }

    @Test
    fun `rejects a start day that does not exist in every month`() {
        assertThatThrownBy { monthPeriod(YearMonth.of(2026, 10), 31) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
