package ai.moneymanager.domain.model

sealed class CategoryUpdateResult {
    data class Updated(val category: Category) : CategoryUpdateResult()
    object Duplicate : CategoryUpdateResult()
    object NotFound : CategoryUpdateResult()
}
