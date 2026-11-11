package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.chat.reply.common.DOT_SEPARATOR
import ai.moneymanager.chat.reply.common.NEGATIVE_SIGN
import ai.moneymanager.chat.reply.common.POSITIVE_SIGN
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.digest.changePercent
import ai.moneymanager.chat.reply.digest.dailyAverage
import ai.moneymanager.chat.reply.digest.monthName
import ai.moneymanager.chat.reply.digest.perDay
import ai.moneymanager.chat.reply.digest.sharePercent
import ai.moneymanager.chat.reply.digest.weekDay
import ai.moneymanager.chat.telegram.widget.WidgetHtml.hero
import ai.moneymanager.chat.telegram.widget.WidgetHtml.row
import ai.moneymanager.chat.telegram.widget.WidgetHtml.span
import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.localeFor
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val DAY_ICON = "🌙"
private const val WEEK_ICON = "🗓"
private const val MONTH_ICON = "📊"
private const val OVER_ICON = "⚠️"
private const val DAY_PATTERN = "d MMMM"
private const val RANGE_SEPARATOR = " — "
private const val SHORT_TOP = 3
private const val LEGEND_TOP = 4
private const val OTHER_COLOR = "rgba(139,154,168,.35)"
private const val OVER_CLASS = "x"
private const val GOOD_CLASS = "v"
private val PALETTE = listOf("#64b5ef", "#4fd18b", "#f0b34a", "#b46fd6")
private val HUNDRED = BigDecimal(100)

@Component
class DigestCardRenderer(private val localizationService: LocalizationService) {
    fun render(digest: Digest, language: String?): String = WidgetHtml.card(
        when (digest.period.kind) {
            DigestKind.DAY -> day(digest, language)
            DigestKind.WEEK -> week(digest, language)
            DigestKind.MONTH -> month(digest, language)
        }
    )

    private fun day(digest: Digest, language: String?): String = buildString {
        append(span("l", "$DAY_ICON ${digest.period.start.format(DateTimeFormatter.ofPattern(DAY_PATTERN, localeFor(language)))}"))
        append(hero(money(digest, digest.spent), digest.spent))
        append(categories(digest))
        append(monthLeft(digest, language, t("widget.tomorrow", language)) { it.perDay() })
    }

    private fun week(digest: Digest, language: String?): String = buildString {
        append(span("l", "$WEEK_ICON ${weekDay(digest.period.start, language)}$RANGE_SEPARATOR${weekDay(digest.period.end, language)}"))
        append(hero(money(digest, digest.spent), digest.spent))
        append(span("s", t("widget.per_day", language, money(digest, digest.dailyAverage()))))
        append(categories(digest))
        append(monthLeft(digest, language, t("widget.month_left", language)) { it.amount })
    }

    private fun month(digest: Digest, language: String?): String = buildString {
        append(span("l", "$MONTH_ICON ${monthName(digest, language)}"))
        append(hero(money(digest, digest.spent), digest.spent))
        digest.changePercent()?.let { append(change(it, language)) }
        append(shares(digest))
        append(balance(digest, language))
        digest.budgets.filter { it.remaining.signum() < 0 }.forEach { budget ->
            append(row("<span>$OVER_ICON ${label(budget.category.icon, budget.category.name)}</span>", "<b class=\"$OVER_CLASS\">$POSITIVE_SIGN${money(digest, budget.remaining.negate())}</b>"))
        }
    }

    private fun categories(digest: Digest): String {
        val top = digest.topExpenses.take(SHORT_TOP)
        val max = top.maxOfOrNull { it.total }?.takeIf { it.signum() > 0 } ?: return ""
        val rows = top.joinToString("") { category ->
            val width = category.total.multiply(HUNDRED).divide(max, 0, RoundingMode.HALF_UP)
            "<div class=\"c\"><span>${escapeHtml(category.icon)}</span><span>${escapeHtml(category.name)} <em>$DOT_SEPARATOR${digest.sharePercent(category)}%</em></span><b>${money(digest, category.total)}</b>" +
                "<div class=\"b\"><i style=\"width:$width%\"></i></div></div>"
        }
        return "<div>$rows</div>"
    }

    private fun shares(digest: Digest): String {
        val top = digest.topExpenses.take(LEGEND_TOP)
        if (top.isEmpty() || digest.spent.signum() <= 0) return ""
        val shares = top.map { digest.sharePercent(it) }
        val other = (HUNDRED - shares.fold(BigDecimal.ZERO, BigDecimal::add)).max(BigDecimal.ZERO)
        val segments = shares.mapIndexed { index, share -> segment(share, PALETTE[index]) } + listOfNotNull(other.takeIf { it.signum() > 0 }?.let { segment(it, OTHER_COLOR) })
        val legend = top.zip(shares).mapIndexed { index, (category, share) -> legendItem(category, share, PALETTE[index]) }
        return "<div class=\"k\">${segments.joinToString("")}</div><div class=\"e\">${legend.joinToString("")}</div>"
    }

    private fun segment(share: BigDecimal, color: String) = "<i style=\"flex:$share;background:$color\"></i>"

    private fun legendItem(category: CategoryTotal, share: BigDecimal, color: String) =
        "<span><em style=\"background:$color\"></em>${escapeHtml(category.name)}<b>$share%</b></span>"

    private fun change(percent: Int, language: String?): String {
        val sign = if (percent > 0) POSITIVE_SIGN else if (percent < 0) NEGATIVE_SIGN else ""
        val tone = if (percent > 0) "u $OVER_CLASS" else "u"
        return span(tone, t("widget.change", language, "$sign${abs(percent)}"))
    }

    private fun balance(digest: Digest, language: String?): String {
        if (digest.income.signum() <= 0) return ""
        val left = digest.income - digest.spent
        val leftValue = if (left.signum() >= 0) "<b class=\"$GOOD_CLASS\">${money(digest, left)}</b>" else "<b class=\"$OVER_CLASS\">$NEGATIVE_SIGN${money(digest, left.negate())}</b>"
        return "<div class=\"q\"><div><span>${t("widget.income", language)}</span><b>${money(digest, digest.income)}</b></div>" +
            "<div><span>${t("widget.left", language)}</span>$leftValue</div></div>"
    }

    private fun monthLeft(digest: Digest, language: String?, label: String, value: (MonthLeft) -> BigDecimal): String {
        val left = digest.monthLeft ?: return ""
        if (left.amount.signum() < 0) return row(span("m", t("widget.overspent", language)), "<b class=\"$OVER_CLASS\">${money(digest, left.amount.negate())}</b>")
        return row(span("m", label), "<b>${money(digest, value(left))}</b>")
    }

    private fun label(icon: String?, name: String): String = escapeHtml(formatIconPrefix(icon) + name)

    private fun money(digest: Digest, amount: BigDecimal): String = formatAmount(amount, digest.group.currency)

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
