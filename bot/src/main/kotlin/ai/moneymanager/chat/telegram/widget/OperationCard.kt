package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.chat.reply.common.DOT_SEPARATOR
import ai.moneymanager.chat.reply.common.categoryButtonText
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.domain.text.formatNumber
import ai.moneymanager.service.LocalizationService
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val STYLE_PATH = "widgets/card.css"
private const val TODAY_KEY = "widget.today"
private const val BUDGET_KEY = "widget.budget"
private const val FULL_PERCENT = 100

data class OperationCard(
    val fallbackText: String,
    val type: CategoryType,
    val amount: BigDecimal,
    val currency: Currency,
    val category: Category,
    val description: String?,
    val date: LocalDate,
    val today: LocalDate,
    val budget: Budget?,
    val language: String?,
)

@Component
class OperationCardRenderer(private val localizationService: LocalizationService) {
    private val style: String = ClassPathResource(STYLE_PATH).inputStream.use { it.readBytes().decodeToString() }

    fun render(cards: List<OperationCard>): String {
        val body = if (cards.size == 1) single(cards.first()) else list(cards)
        return "<style>$style</style><div class=\"w\">$body</div>"
    }

    private fun single(card: OperationCard): String = buildString {
        append(span("l", escapeHtml(categoryButtonText(card.category))))
        append(span(if (card.type == CategoryType.INCOME) "h p" else "h", signed(card)))
        append(span("s", details(card)))
        card.budget?.let { append(budget(it, card)) }
    }

    private fun list(cards: List<OperationCard>): String = cards.joinToString("") { card ->
        "<div class=\"r\"><span>${escapeHtml(categoryButtonText(card.category.copy(name = card.description ?: card.category.name)))}</span><b>${signed(card)}</b></div>"
    }

    private fun details(card: OperationCard): String {
        val day = if (card.date == card.today) localizationService.t(TODAY_KEY, card.language) else card.date.format(shortDateFormatter)
        return listOfNotNull(card.description?.let(::escapeHtml), day).joinToString(DOT_SEPARATOR)
    }

    private fun budget(budget: Budget, card: OperationCard): String {
        val width = budget.percent.coerceAtMost(FULL_PERCENT)
        val tone = when {
            budget.percent >= BudgetThreshold.OVER.percent -> " class=\"o\""
            budget.percent >= BudgetThreshold.NEAR.percent -> " class=\"n\""
            else -> ""
        }
        return "<div class=\"r\"><span class=\"m\">${localizationService.t(BUDGET_KEY, card.language)}</span>" +
            "<span><b>${formatNumber(budget.spent)}</b> <span class=\"m\">/ ${formatAmount(budget.effectiveLimit, card.currency)}</span></span></div>" +
            "<div class=\"t\"><i$tone style=\"width:$width%\"></i></div>"
    }

    private fun signed(card: OperationCard): String = formatSignedAmount(card.type, card.amount, card.currency)

    private fun span(className: String, content: String) = "<span class=\"$className\">$content</span>"
}
