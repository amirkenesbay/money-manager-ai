package ai.moneymanager.domain.model

sealed class CategoryCreationResult {
    data class Created(val category: Category) : CategoryCreationResult()
    object Duplicate : CategoryCreationResult()
    data class LimitReached(val limit: Int) : CategoryCreationResult()
}
