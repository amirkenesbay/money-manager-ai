package ai.moneymanager.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

const val DEFAULT_MONTH_START_DAY = 1
val MONTH_START_DAYS = 1..28

data class MonthPeriod(val month: YearMonth, val start: LocalDate, val end: LocalDate) {
    val days: Int
        get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    fun dateOf(dayOfMonth: Int): LocalDate =
        if (dayOfMonth >= start.dayOfMonth) start.withDayOfMonth(dayOfMonth) else end.withDayOfMonth(dayOfMonth)
}

fun periodStartingAt(start: LocalDate): MonthPeriod =
    MonthPeriod(YearMonth.from(start), start, start.plusMonths(1).minusDays(1))

fun monthPeriod(month: YearMonth, startDay: Int): MonthPeriod {
    require(startDay in MONTH_START_DAYS) { "Month start day must be in $MONTH_START_DAYS" }
    return periodStartingAt(month.atDay(startDay))
}

fun currentMonthPeriod(today: LocalDate, startDay: Int): MonthPeriod {
    val month = YearMonth.from(today)
    return monthPeriod(if (today.dayOfMonth >= startDay) month else month.minusMonths(1), startDay)
}
