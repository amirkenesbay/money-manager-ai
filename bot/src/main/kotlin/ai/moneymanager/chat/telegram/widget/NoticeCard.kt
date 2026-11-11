package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.chat.reply.common.DOT_SEPARATOR
import ai.moneymanager.chat.reply.common.POSITIVE_SIGN
import ai.moneymanager.chat.reply.common.categoryButtonText
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.iconLabel
import ai.moneymanager.chat.telegram.widget.WidgetHtml.hero
import ai.moneymanager.chat.telegram.widget.WidgetHtml.person
import ai.moneymanager.chat.telegram.widget.WidgetHtml.row
import ai.moneymanager.chat.telegram.widget.WidgetHtml.span
import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.daysAfter
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.domain.text.formatNumber
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.LocalizationService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate

private const val RING_LENGTH = 176.0
private const val FULL_PERCENT = 100
private const val NEAR_TONE = "n"
private const val OVER_TONE = "o"

@Component
class NoticeCardRenderer(
    private val localizationService: LocalizationService,
    private val clock: Clock,
) {
    fun budget(budget: Budget, threshold: BudgetThreshold, group: MoneyGroup, language: String?): String {
        val over = threshold == BudgetThreshold.OVER
        val limit = formatAmount(budget.effectiveLimit, group.currency)
        val leftText = if (over) "$POSITIVE_SIGN${formatAmount(budget.remaining.negate(), group.currency)}" else formatAmount(budget.remaining, group.currency)
        val leftNote = if (over) t("widget.over_of", language, limit) else t("widget.left_per_day", language, formatAmount(perDay(budget), group.currency))
        val spent = budget.percent.coerceIn(0, FULL_PERCENT)
        return WidgetHtml.card(
            "<div class=\"y\"><span>${ring(budget.percent, spent, over)}</span><span class=\"z\">" +
                span("l", "${escapeHtml(categoryButtonText(budget.category))}$DOT_SEPARATOR${escapeHtml(group.name)}") +
                "<b${if (over) " class=\"x\"" else ""} data-a=\"$leftText\" data-b=\"${formatAmount(budget.spent, group.currency)}\">$leftText</b>" +
                "<span class=\"s\" data-a=\"$leftNote\" data-b=\"${t("widget.spent_of", language, limit)}\">$leftNote</span></span></div>"
        )
    }

    fun categoryCreated(actorName: String, group: MoneyGroup, icon: String?, name: String, language: String?): String = WidgetHtml.card(
        person(actorName, group.name, DOT_SEPARATOR) +
            "<div class=\"n\"><span class=\"j\">${escapeHtml(icon ?: DEFAULT_CATEGORY_ICON)}</span><span>" +
            span("l", t("widget.new_category", language)) + "<br>" + span("nm", escapeHtml(name)) + "</span></div>"
    )

    fun added(changes: List<OperationChange>, actorName: String, group: MoneyGroup, budgets: List<Budget>, language: String?): String? {
        val operations = changes.map { (it as? OperationChange.Added)?.operation ?: return null }
        val body = operations.singleOrNull()?.let { single(it, group.currency, budgets, language) } ?: operations.joinToString("") { line(it, group.currency) }
        return WidgetHtml.card(person(actorName, group.name, DOT_SEPARATOR) + body)
    }

    private fun single(operation: FinanceOperationEntity, currency: Currency, budgets: List<Budget>, language: String?): String {
        val budget = budgets.find { it.category.id == operation.categoryId }
        val chip = span("p", listOfNotNull(label(operation), operation.description?.let(::escapeHtml)).joinToString(DOT_SEPARATOR))
        return hero(signed(operation, currency), operation.amount) + chip + (budget?.let { budgetLine(it, currency, language) } ?: "")
    }

    private fun budgetLine(budget: Budget, currency: Currency, language: String?): String {
        val tone = when {
            budget.percent >= BudgetThreshold.OVER.percent -> " class=\"$OVER_TONE\""
            budget.percent >= BudgetThreshold.NEAR.percent -> " class=\"$NEAR_TONE\""
            else -> ""
        }
        val amounts = "<b>${formatNumber(budget.spent)} / ${formatAmount(budget.effectiveLimit, currency)}</b>"
        return row("<span>${t("widget.budget", language)}</span>", amounts) +
            "<div class=\"t\"><i$tone style=\"width:${budget.percent.coerceIn(0, FULL_PERCENT)}%\"></i></div>"
    }

    private fun line(operation: FinanceOperationEntity, currency: Currency): String =
        row("<span>${label(operation)}</span>", "<b>${signed(operation, currency)}</b>")

    private fun label(operation: FinanceOperationEntity): String = escapeHtml(iconLabel(operation.categoryIcon, operation.categoryName))

    private fun signed(operation: FinanceOperationEntity, currency: Currency): String = formatSignedAmount(operation.type, operation.amount, currency)

    private fun perDay(budget: Budget): BigDecimal {
        val days = daysAfter(LocalDate.now(clock), budget.period) + 1
        return budget.remaining.max(BigDecimal.ZERO).divide(BigDecimal(days.coerceAtLeast(1)), 0, RoundingMode.DOWN)
    }

    private fun ring(percent: Int, spent: Int, over: Boolean): String {
        val offset = BigDecimal(RING_LENGTH * (FULL_PERCENT - spent) / FULL_PERCENT).setScale(1, RoundingMode.HALF_UP)
        return "<svg viewBox=\"0 0 68 68\" width=\"68\" height=\"68\"><circle class=\"rb\" cx=\"34\" cy=\"34\" r=\"28\"/>" +
            "<circle class=\"rf${if (over) " $OVER_TONE" else ""}\" cx=\"34\" cy=\"34\" r=\"28\" stroke-dasharray=\"$RING_LENGTH\" stroke-dashoffset=\"$offset\"/>" +
            "<text x=\"34\" y=\"34\">$percent%</text></svg>"
    }

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
