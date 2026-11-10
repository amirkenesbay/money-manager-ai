package ai.moneymanager.chat.reply.digest

import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.service.localeFor
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private const val WEEK_DAY_PATTERN = "d MMM"
private const val ABBREVIATION_DOT = "."
private val HUNDRED = BigDecimal(100)

fun Digest.changePercent(): Int? {
    val previous = previousSpent?.takeIf { it.signum() > 0 } ?: return null
    return (spent - previous).multiply(HUNDRED).divide(previous, 0, RoundingMode.HALF_UP).toInt()
}

fun Digest.sharePercent(category: CategoryTotal): BigDecimal =
    if (spent.signum() > 0) category.total.multiply(HUNDRED).divide(spent, 0, RoundingMode.HALF_UP) else BigDecimal.ZERO

fun Digest.dailyAverage(): BigDecimal {
    val days = period.end.toEpochDay() - period.start.toEpochDay() + 1
    return spent.divide(BigDecimal(days), 0, RoundingMode.HALF_UP)
}

fun MonthLeft.perDay(): BigDecimal = amount.divide(BigDecimal(days), 0, RoundingMode.DOWN)

fun monthName(digest: Digest, language: String?): String =
    digest.period.start.month.getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language)).replaceFirstChar { it.uppercaseChar() }

fun weekDay(date: LocalDate, language: String?): String =
    date.format(DateTimeFormatter.ofPattern(WEEK_DAY_PATTERN, localeFor(language))).removeSuffix(ABBREVIATION_DOT)
