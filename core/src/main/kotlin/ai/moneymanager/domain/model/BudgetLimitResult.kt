package ai.moneymanager.domain.model

sealed class BudgetLimitResult {
    data class Updated(val category: Category) : BudgetLimitResult()
    data class LimitReached(val limit: Int) : BudgetLimitResult()
    object NotExpense : BudgetLimitResult()
    object NotFound : BudgetLimitResult()
}
