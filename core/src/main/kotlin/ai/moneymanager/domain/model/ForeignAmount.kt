package ai.moneymanager.domain.model

import java.math.BigDecimal

data class ForeignAmount(
    val amount: BigDecimal,
    val currency: Currency,
)
