package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate

const val RATE_SCALE = 10

fun interface KztRatesSource {
    fun fetch(date: LocalDate): Map<Currency, BigDecimal>
}
