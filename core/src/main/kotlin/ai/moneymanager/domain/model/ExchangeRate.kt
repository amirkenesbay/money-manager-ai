package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class ExchangeRate(
    val from: Currency,
    val to: Currency,
    val rate: BigDecimal,
    val date: LocalDate,
)
