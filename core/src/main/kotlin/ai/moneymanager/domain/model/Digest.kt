package ai.moneymanager.domain.model

import ai.moneymanager.domain.model.report.CategoryTotal
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private val DAY_DIGEST_TIME: LocalTime = LocalTime.of(21, 0)
private val WEEK_DIGEST_TIME: LocalTime = LocalTime.of(20, 0)
private val MONTH_DIGEST_TIME: LocalTime = LocalTime.of(10, 0)
private val WEEK_DIGEST_DAY = DayOfWeek.SUNDAY
private const val DAYS_IN_WEEK = 7L
private const val CATCH_UP_DAYS = 2L

enum class DigestKind {
    DAY,
    WEEK,
    MONTH,
}

data class DigestPeriod(val kind: DigestKind, val start: LocalDate, val end: LocalDate)

data class MonthLeft(val month: YearMonth, val amount: BigDecimal, val days: Int)

data class Digest(
    val period: DigestPeriod,
    val group: MoneyGroup,
    val spent: BigDecimal,
    val income: BigDecimal,
    val operationCount: Int,
    val topExpenses: List<CategoryTotal>,
    val previousSpent: BigDecimal?,
    val budgets: List<Budget>,
    val monthLeft: MonthLeft?,
)

fun dueDigestPeriod(kind: DigestKind, now: LocalDateTime, monthStartDay: Int): DigestPeriod? = when (kind) {
    DigestKind.DAY -> now.toLocalDate().takeIf { now.toLocalTime() >= DAY_DIGEST_TIME }?.let { DigestPeriod(kind, it, it) }
    DigestKind.WEEK -> {
        val weekEnd = now.toLocalDate().with(TemporalAdjusters.previousOrSame(WEEK_DIGEST_DAY))
        DigestPeriod(kind, weekEnd.minusDays(DAYS_IN_WEEK - 1), weekEnd).takeIf { withinCatchUp(now, weekEnd.atTime(WEEK_DIGEST_TIME)) }
    }
    DigestKind.MONTH -> {
        val current = currentMonthPeriod(now.toLocalDate(), monthStartDay)
        val previous = periodStartingAt(current.start.minusMonths(1))
        DigestPeriod(kind, previous.start, previous.end).takeIf { withinCatchUp(now, current.start.atTime(MONTH_DIGEST_TIME)) }
    }
}

fun daysAfter(today: LocalDate, period: MonthPeriod): Int = ChronoUnit.DAYS.between(today, period.end).toInt()

private fun withinCatchUp(now: LocalDateTime, sendAt: LocalDateTime): Boolean =
    !now.isBefore(sendAt) && now.isBefore(sendAt.plusDays(CATCH_UP_DAYS))
