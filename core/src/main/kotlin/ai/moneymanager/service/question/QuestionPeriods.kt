package ai.moneymanager.service.question

import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.monthPeriod
import ai.moneymanager.domain.model.periodStartingAt
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.model.question.PeriodSpec
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val DAYS_IN_WEEK = 7L
private const val QUARTER_MONTHS = 3L

data class QuestionRange(val from: LocalDate?, val to: LocalDate, val month: MonthPeriod? = null) {
    fun contains(date: LocalDate): Boolean = (from == null || !date.isBefore(from)) && !date.isAfter(to)
}

fun resolvePeriod(spec: PeriodSpec?, today: LocalDate, startDay: Int, fallback: PeriodPreset = PeriodPreset.THIS_MONTH): QuestionRange =
    when (val period = spec ?: PeriodSpec.Preset(fallback)) {
        is PeriodSpec.Preset -> preset(period.preset, today, startDay)
        is PeriodSpec.Month -> monthPeriod(period.month, startDay).let { QuestionRange(it.start, it.end, it) }
        is PeriodSpec.Range -> QuestionRange(minOf(period.from, period.to), maxOf(period.from, period.to))
    }

fun previousPeriod(range: QuestionRange): QuestionRange {
    range.month?.let { current -> return periodStartingAt(current.start.minusMonths(1)).let { QuestionRange(it.start, it.end, it) } }
    val from = range.from ?: return range
    val days = range.to.toEpochDay() - from.toEpochDay() + 1
    return QuestionRange(from.minusDays(days), from.minusDays(1))
}

private fun preset(preset: PeriodPreset, today: LocalDate, startDay: Int): QuestionRange {
    val currentMonth = currentMonthPeriod(today, startDay)
    return when (preset) {
        PeriodPreset.TODAY -> QuestionRange(today, today)
        PeriodPreset.YESTERDAY -> today.minusDays(1).let { QuestionRange(it, it) }
        PeriodPreset.THIS_WEEK -> QuestionRange(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today)
        PeriodPreset.LAST_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(DAYS_IN_WEEK)
            .let { QuestionRange(it, it.plusDays(DAYS_IN_WEEK - 1)) }
        PeriodPreset.THIS_MONTH -> QuestionRange(currentMonth.start, currentMonth.end, currentMonth)
        PeriodPreset.LAST_MONTH -> periodStartingAt(currentMonth.start.minusMonths(1)).let { QuestionRange(it.start, it.end, it) }
        PeriodPreset.LAST_3_MONTHS -> QuestionRange(currentMonth.start.minusMonths(QUARTER_MONTHS - 1), currentMonth.end)
        PeriodPreset.THIS_YEAR -> QuestionRange(today.withDayOfYear(1), today)
        PeriodPreset.LAST_YEAR -> today.minusYears(1).let { QuestionRange(it.withDayOfYear(1), it.withDayOfYear(it.lengthOfYear())) }
        PeriodPreset.ALL -> QuestionRange(null, today)
    }
}
