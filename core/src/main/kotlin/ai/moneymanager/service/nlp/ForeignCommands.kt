package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.ForeignAmount
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.text.resolveOperationDate
import ai.moneymanager.service.fx.ExchangeRateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class ForeignCommands(
    private val exchangeRateService: ExchangeRateService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun inGroupCurrency(commands: List<BotCommand>, groupCurrency: Currency, today: LocalDate): List<BotCommand> =
        commands.map { command ->
            when (command) {
                is BotCommand.AddExpense -> converted(command.amount, command.currency, command.operationDate, groupCurrency, today)
                    ?.let { (amount, original) -> command.copy(amount = amount, currency = null, original = original) } ?: command
                is BotCommand.AddIncome -> converted(command.amount, command.currency, command.operationDate, groupCurrency, today)
                    ?.let { (amount, original) -> command.copy(amount = amount, currency = null, original = original) } ?: command
                else -> command
            }
        }

    private fun converted(amount: Double, currency: Currency?, dateText: String?, groupCurrency: Currency, today: LocalDate): Pair<Double, ForeignAmount>? {
        if (currency == null || currency == groupCurrency) return null
        val spoken = BigDecimal.valueOf(amount)
        val date = resolveOperationDate(dateText, today)
        val inGroupCurrency = exchangeRateService.convert(spoken, currency, groupCurrency, date)
        if (inGroupCurrency == null) log.warn("No exchange rate for a spoken amount: from={}, to={}, date={}", currency, groupCurrency, date)
        return inGroupCurrency?.let { it.toDouble() to ForeignAmount(spoken, currency) }
    }
}
