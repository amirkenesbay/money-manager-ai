package ai.moneymanager.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurrenceTest {
    private val monthly = Recurrence(RecurrenceUnit.MONTH)

    @Test
    fun `keeps the end of the month without drifting`() {
        val start = LocalDate.of(2026, 1, 31)

        assertThat(monthly.nextAfter(start, start)).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(monthly.nextAfter(start, LocalDate.of(2026, 2, 28))).isEqualTo(LocalDate.of(2026, 3, 31))
    }

    @Test
    fun `starts on the start date and steps by the interval`() {
        val start = LocalDate.of(2026, 11, 15)
        val fortnightly = Recurrence(RecurrenceUnit.WEEK, 2)

        assertThat(fortnightly.nextAfter(start, start.minusDays(3))).isEqualTo(start)
        assertThat(fortnightly.nextAfter(start, start)).isEqualTo(LocalDate.of(2026, 11, 29))
        assertThat(fortnightly.nextAfter(start, LocalDate.of(2026, 12, 1))).isEqualTo(LocalDate.of(2026, 12, 13))
    }

    @Test
    fun `turns any schedule into a monthly amount`() {
        assertThat(monthly.monthly(BigDecimal("3500"))).isEqualByComparingTo("3500")
        assertThat(Recurrence(RecurrenceUnit.YEAR).monthly(BigDecimal("12000"))).isEqualByComparingTo("1000")
        assertThat(Recurrence(RecurrenceUnit.MONTH, 3).monthly(BigDecimal("9000"))).isEqualByComparingTo("3000")
        assertThat(Recurrence(RecurrenceUnit.WEEK).monthly(BigDecimal("1000"))).isEqualByComparingTo("4348.13")
    }

    @Test
    fun `refuses a meaningless interval`() {
        assertThatThrownBy { Recurrence(RecurrenceUnit.DAY, 0) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
