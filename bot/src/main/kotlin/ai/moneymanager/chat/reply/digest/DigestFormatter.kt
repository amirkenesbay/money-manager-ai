package ai.moneymanager.chat.reply.digest

import ai.moneymanager.chat.reply.common.blockquote
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.expandableBlockquote
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.localeFor
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private const val WEEK_DAY_PATTERN = "d MMM"
private const val ABBREVIATION_DOT = "."
private const val LINE_BREAK = "\n"
private const val PARAGRAPH_BREAK = "\n\n"
private val HUNDRED = BigDecimal(100)

@Component
class DigestFormatter(
    private val localizationService: LocalizationService,
) {
    fun format(digest: Digest, language: String?): String = when (digest.period.kind) {
        DigestKind.DAY -> day(digest, language)
        DigestKind.WEEK -> week(digest, language)
        DigestKind.MONTH -> month(digest, language)
    }

    fun muteButton(kind: DigestKind, language: String?): String = t("digest.mute.${kind.name.lowercase()}", language)

    fun mutedConfirmation(kind: DigestKind, language: String?): String = t("digest.muted.${kind.name.lowercase()}", language)

    private fun day(digest: Digest, language: String?): String = paragraphs(
        lines(
            t("digest.day.title", language, groupName(digest)),
            "",
            t("digest.spent", language, money(digest, digest.spent)),
            t("digest.day.count", language, digest.operationCount),
            topSingle(digest, language),
        ),
        monthLeft(digest, language) { left -> t("digest.tomorrow", language, money(digest, perDay(left))) },
        t("digest.day.forgot", language),
    )

    private fun week(digest: Digest, language: String?): String {
        val days = digest.period.end.toEpochDay() - digest.period.start.toEpochDay() + 1
        return paragraphs(
            lines(
                t("digest.week.title", language, weekDay(digest.period.start, language), weekDay(digest.period.end, language), groupName(digest)),
                "",
                t("digest.spent", language, money(digest, digest.spent)),
                t("digest.average", language, money(digest, digest.spent.divide(BigDecimal(days), 0, RoundingMode.HALF_UP))),
                topSingle(digest, language),
            ),
            monthLeft(digest, language) { left ->
                t("digest.month_left", language, money(digest, left.amount), money(digest, perDay(left)))
            },
        )
    }

    private fun month(digest: Digest, language: String?): String = paragraphs(
        lines(
            t("digest.month.title", language, monthName(digest, language), groupName(digest)),
            "",
            t("digest.spent", language, money(digest, digest.spent)),
            comparison(digest, language),
        ),
        lines(*balance(digest, language).toTypedArray()),
        topExpenses(digest, language),
        budgets(digest, language),
    )

    private fun comparison(digest: Digest, language: String?): String? {
        val previous = digest.previousSpent ?: return null
        val change = (digest.spent - previous).multiply(HUNDRED).divide(previous, 0, RoundingMode.HALF_UP).toInt()
        return when {
            change < 0 -> t("digest.compare.less", language, -change)
            change > 0 -> t("digest.compare.more", language, change)
            else -> t("digest.compare.same", language)
        }
    }

    private fun balance(digest: Digest, language: String?): List<String> {
        if (digest.income.signum() <= 0) return emptyList()
        val left = digest.income - digest.spent
        val leftLine = if (left.signum() >= 0) t("digest.left", language, money(digest, left)) else t("digest.overspent", language, money(digest, left.negate()))
        return listOf(t("digest.income", language, money(digest, digest.income)), leftLine)
    }

    private fun topExpenses(digest: Digest, language: String?): String? {
        if (digest.topExpenses.isEmpty() || digest.spent.signum() <= 0) return null
        val rows = digest.topExpenses.map { category ->
            val share = category.total.multiply(HUNDRED).divide(digest.spent, 0, RoundingMode.HALF_UP)
            t("digest.top.line", language, categoryLabel(category), money(digest, category.total), share)
        }
        return expandableBlockquote(lines(t("digest.top.title", language), *rows.toTypedArray()))
    }

    private fun budgets(digest: Digest, language: String?): String? {
        if (digest.budgets.isEmpty()) return null
        val over = digest.budgets.filter { it.remaining.signum() < 0 }
        val fine = digest.budgets.size - over.size
        val overLines = over.map { t("digest.budgets.over", language, budgetLabel(it), money(digest, it.remaining.negate())) }
        val fineLine = t("digest.budgets.fine", language, fine, digest.budgets.size).takeIf { fine > 0 }
        return blockquote(lines(t("digest.budgets.title", language), fineLine, *overLines.toTypedArray()))
    }

    private fun topSingle(digest: Digest, language: String?): String? =
        digest.topExpenses.firstOrNull()?.let { t("digest.top.single", language, categoryLabel(it), money(digest, it.total)) }

    private fun monthLeft(digest: Digest, language: String?, line: (MonthLeft) -> String): String? {
        val left = digest.monthLeft ?: return null
        if (left.amount.signum() < 0) return blockquote(t("digest.month_overspent", language, money(digest, left.amount.negate())))
        return blockquote(line(left))
    }

    private fun perDay(left: MonthLeft): BigDecimal = left.amount.divide(BigDecimal(left.days), 0, RoundingMode.DOWN)

    private fun monthName(digest: Digest, language: String?): String =
        digest.period.start.month.getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language)).replaceFirstChar { it.uppercaseChar() }

    private fun weekDay(date: LocalDate, language: String?): String =
        date.format(DateTimeFormatter.ofPattern(WEEK_DAY_PATTERN, localeFor(language))).removeSuffix(ABBREVIATION_DOT)

    private fun groupName(digest: Digest): String = escapeHtml(digest.group.name)

    private fun categoryLabel(category: CategoryTotal): String = escapeHtml(formatIconPrefix(category.icon) + category.name)

    private fun budgetLabel(budget: Budget): String = escapeHtml(formatIconPrefix(budget.category.icon) + budget.category.name)

    private fun money(digest: Digest, amount: BigDecimal): String = formatAmount(amount, digest.group.currency)

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)

    private fun lines(vararg parts: String?): String = parts.filterNotNull().joinToString(LINE_BREAK)

    private fun paragraphs(vararg parts: String?): String = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(PARAGRAPH_BREAK)
}
