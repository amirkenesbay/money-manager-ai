package ai.moneymanager.chat.reply.loan

import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.service.LocalizationService
import org.springframework.stereotype.Component

@Component
class LoanFormatter(
    private val localizationService: LocalizationService,
) {
    fun created(loan: LoanEntity, currency: Currency, language: String?): String {
        val key = if (loan.direction == LoanDirection.LENT) "loan.lent" else "loan.borrowed"
        val due = loan.dueDate?.let { t("loan.due_suffix", language, it.format(shortDateFormatter)) }.orEmpty()
        return t(key, language, escapeHtml(loan.person), formatAmount(loan.amount, currency), due)
    }

    fun repaid(loan: LoanEntity, currency: Currency, language: String?): String {
        if (!loan.open) return t("loan.closed", language, escapeHtml(loan.person), formatAmount(loan.amount, currency))
        val last = loan.repayments.last()
        return t("loan.repaid", language, escapeHtml(loan.person), formatAmount(last.amount, currency), formatAmount(loan.remaining, currency))
    }

    fun due(loan: LoanEntity, currency: Currency, language: String?): String =
        t("loan.due_today", language, escapeHtml(loan.person), formatAmount(loan.remaining, currency))

    fun notFound(person: String, language: String?): String = t("loan.not_found", language, escapeHtml(person))

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
