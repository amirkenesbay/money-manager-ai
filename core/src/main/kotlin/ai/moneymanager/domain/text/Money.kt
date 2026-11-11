package ai.moneymanager.domain.text

import ai.moneymanager.domain.model.Currency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private const val AMOUNT_PATTERN = "#,##0"
private const val THOUSANDS_SEPARATOR = ' '

private val amountFormat = DecimalFormat(AMOUNT_PATTERN, DecimalFormatSymbols().apply { groupingSeparator = THOUSANDS_SEPARATOR })

fun formatNumber(amount: BigDecimal): String = amountFormat.format(amount)

fun formatAmount(amount: BigDecimal, currency: Currency): String = "${formatNumber(amount)}${currency.symbol}"
