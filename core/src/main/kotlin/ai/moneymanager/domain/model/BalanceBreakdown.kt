package ai.moneymanager.domain.model

import java.math.BigDecimal

data class BalanceBreakdown(
    val initial: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val total: BigDecimal
)
