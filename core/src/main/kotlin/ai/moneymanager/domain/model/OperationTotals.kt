package ai.moneymanager.domain.model

import java.math.BigDecimal

data class OperationTotals(
    val income: BigDecimal,
    val expense: BigDecimal,
    val count: Int,
)
