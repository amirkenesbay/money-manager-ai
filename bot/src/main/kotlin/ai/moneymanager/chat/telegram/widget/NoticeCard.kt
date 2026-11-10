package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.chat.reply.common.DOT_SEPARATOR
import ai.moneymanager.chat.reply.common.POSITIVE_SIGN
import ai.moneymanager.chat.reply.common.categoryButtonText
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.iconLabel
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.chat.telegram.widget.WidgetHtml.row
import ai.moneymanager.chat.telegram.widget.WidgetHtml.span
import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.LocalizationService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private const val RING_LENGTH = 188.5
private const val FULL_PERCENT = 100
private const val UNTIL_KEY = "widget.until"
private const val OF_KEY = "widget.of"

@Component
class NoticeCardRenderer(private val localizationService: LocalizationService) {
    fun budget(budget: Budget, threshold: BudgetThreshold, group: MoneyGroup, language: String?): String {
        val over = threshold == BudgetThreshold.OVER
        val value = if (over) "$POSITIVE_SIGN${formatAmount(budget.remaining.negate(), group.currency)}" else formatAmount(budget.remaining, group.currency)
        val details = listOf(
            localizationService.t(OF_KEY, language, formatAmount(budget.effectiveLimit, group.currency)),
            localizationService.t(UNTIL_KEY, language, budget.period.end.format(shortDateFormatter)),
        ).joinToString(DOT_SEPARATOR)
        return WidgetHtml.card(
            "<div class=\"y\">${ring(budget.percent, over)}<span class=\"z\">" +
                span("l", "${escapeHtml(categoryButtonText(budget.category))}$DOT_SEPARATOR${escapeHtml(group.name)}") +
                "<b${if (over) " class=\"x\"" else ""}>$value</b>" + span("s", details) + "</span></div>"
        )
    }

    fun added(changes: List<OperationChange>, actorName: String, group: MoneyGroup): String? {
        val operations = changes.map { (it as? OperationChange.Added)?.operation ?: return null }
        val header = "<span class=\"f\"><i>${escapeHtml(actorName.take(1).uppercase())}</i>${span("l", "${escapeHtml(actorName)}$DOT_SEPARATOR${escapeHtml(group.name)}")}</span>"
        val body = operations.singleOrNull()?.let { single(it, group.currency) } ?: operations.joinToString("") { line(it, group.currency) }
        return WidgetHtml.card(header + body)
    }

    private fun single(operation: FinanceOperationEntity, currency: Currency): String =
        span("h", signed(operation, currency)) + span("s", listOfNotNull(label(operation), operation.description?.let(::escapeHtml)).joinToString(DOT_SEPARATOR))

    private fun line(operation: FinanceOperationEntity, currency: Currency): String =
        row("<span>${label(operation)}</span>", "<b>${signed(operation, currency)}</b>")

    private fun label(operation: FinanceOperationEntity): String = escapeHtml(iconLabel(operation.categoryIcon, operation.categoryName))

    private fun signed(operation: FinanceOperationEntity, currency: Currency): String = formatSignedAmount(operation.type, operation.amount, currency)

    private fun ring(percent: Int, over: Boolean): String {
        val offset = BigDecimal(RING_LENGTH * (FULL_PERCENT - percent.coerceIn(0, FULL_PERCENT)) / FULL_PERCENT).setScale(1, RoundingMode.HALF_UP)
        return "<svg viewBox=\"0 0 68 68\" width=\"68\" height=\"68\"><circle class=\"rb\" cx=\"34\" cy=\"34\" r=\"30\"/>" +
            "<circle class=\"rf${if (over) " o" else ""}\" cx=\"34\" cy=\"34\" r=\"30\" stroke-dasharray=\"$RING_LENGTH\" stroke-dashoffset=\"$offset\"/>" +
            "<text x=\"34\" y=\"34\">$percent%</text></svg>"
    }
}
