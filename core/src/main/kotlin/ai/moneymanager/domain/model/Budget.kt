package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

private val HUNDRED = BigDecimal(100)

enum class BudgetThreshold(val percent: Int) {
    NEAR(80),
    OVER(100),
}

data class Budget(
    val category: Category,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val period: MonthPeriod,
) {
    val remaining: BigDecimal
        get() = limit - spent

    val percent: Int
        get() = spent.multiply(HUNDRED).divide(limit, 0, RoundingMode.DOWN).toInt()

    val reachedThresholds: List<BudgetThreshold>
        get() = BudgetThreshold.entries.filter { percent >= it.percent }
}
