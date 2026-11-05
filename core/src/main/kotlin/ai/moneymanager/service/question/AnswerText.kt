package ai.moneymanager.service.question

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.localeFor
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private const val SHORT_DATE_PATTERN = "dd.MM"
private const val FULL_DATE_PATTERN = "dd.MM.yyyy"
private const val RANGE_SEPARATOR = " – "
private const val LIST_SEPARATOR = " + "
private const val ICON_SEPARATOR = " "
internal val HUNDRED: BigDecimal = BigDecimal(100)

class AnswerText(
    private val localizationService: LocalizationService,
    private val language: String?,
    private val currency: Currency,
    private val today: LocalDate,
) {
    fun t(key: String, vararg args: Any): String = localizationService.t("ask.$key", language, *args)

    fun money(amount: BigDecimal): String = formatAmount(amount, currency)

    fun date(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) SHORT_DATE_PATTERN else FULL_DATE_PATTERN))

    fun period(range: QuestionRange): String {
        val from = range.from ?: return t("period.all")
        range.month?.takeIf { it.start == from && it.end == range.to && it.start.dayOfMonth == 1 }?.let { month ->
            val name = month.month.month.getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language)).replaceFirstChar { it.uppercaseChar() }
            return if (month.month.year == today.year) name else "$name ${month.month.year}"
        }
        if (from == range.to) return date(from)
        return date(from) + RANGE_SEPARATOR + date(range.to)
    }

    fun categories(categories: List<Category>): String = categories.joinToString(LIST_SEPARATOR) { category(it) }

    fun category(category: Category): String = icon(category.icon) + ICON_SEPARATOR + category.name

    fun icon(icon: String?): String = icon?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY_ICON

    fun percent(part: BigDecimal, whole: BigDecimal): Int =
        if (whole.signum() == 0) 0 else part.multiply(HUNDRED).divide(whole, 0, RoundingMode.HALF_UP).toInt()

    fun change(current: BigDecimal, previous: BigDecimal): String {
        if (previous.signum() == 0) return t("compare.new", money(previous))
        val change = percent(current - previous, previous)
        return when {
            change > 0 -> t("compare.more", change, money(previous))
            change < 0 -> t("compare.less", -change, money(previous))
            else -> t("compare.same", money(previous))
        }
    }
}
