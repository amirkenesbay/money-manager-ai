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
    val carriedOver: BigDecimal = BigDecimal.ZERO,
) {
    val effectiveLimit: BigDecimal
        get() = limit + carriedOver

    val remaining: BigDecimal
        get() = effectiveLimit - spent

    val percent: Int
        get() = if (effectiveLimit.signum() > 0) {
            spent.multiply(HUNDRED).divide(effectiveLimit, 0, RoundingMode.DOWN).toInt()
        } else {
            BudgetThreshold.OVER.percent
        }

    val reachedThresholds: List<BudgetThreshold>
        get() = BudgetThreshold.entries.filter { percent >= it.percent }
}
