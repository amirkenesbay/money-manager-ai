package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MAX_INTERVAL = 365
private val DAYS_IN_MONTH = BigDecimal("30.4375")
private val WEEKS_IN_MONTH = BigDecimal("4.348125")
private const val MONTHS_IN_YEAR = 12
private const val MONEY_SCALE = 2

enum class RecurrenceUnit(val chronoUnit: ChronoUnit) {
    DAY(ChronoUnit.DAYS),
    WEEK(ChronoUnit.WEEKS),
    MONTH(ChronoUnit.MONTHS),
    YEAR(ChronoUnit.YEARS),
}

data class Recurrence(val unit: RecurrenceUnit, val interval: Int = 1) {
    init {
        require(interval in 1..MAX_INTERVAL) { "Recurrence interval must be in 1..$MAX_INTERVAL" }
    }

    fun occurrence(start: LocalDate, index: Long): LocalDate = start.plus(index * interval, unit.chronoUnit)

    fun nextAfter(start: LocalDate, date: LocalDate): LocalDate {
        if (date.isBefore(start)) return start
        val steps = unit.chronoUnit.between(start, date) / interval
        return generateSequence(steps) { it + 1 }.map { occurrence(start, it) }.first { it.isAfter(date) }
    }

    fun firstOnOrAfter(start: LocalDate, date: LocalDate): LocalDate = nextAfter(start, date.minusDays(1))

    fun monthly(amount: BigDecimal): BigDecimal {
        val perMonth = when (unit) {
            RecurrenceUnit.DAY -> amount.multiply(DAYS_IN_MONTH)
            RecurrenceUnit.WEEK -> amount.multiply(WEEKS_IN_MONTH)
            RecurrenceUnit.MONTH -> amount
            RecurrenceUnit.YEAR -> amount.divide(BigDecimal(MONTHS_IN_YEAR), MONEY_SCALE, RoundingMode.HALF_UP)
        }
        return perMonth.divide(BigDecimal(interval), MONEY_SCALE, RoundingMode.HALF_UP)
    }
}
