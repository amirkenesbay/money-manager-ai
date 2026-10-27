package ai.moneymanager.domain.model.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import java.time.LocalDate

sealed class OperationSuggestion {
    data class Ready(
        val type: CategoryType,
        val amount: Double,
        val category: Category,
        val description: String?,
        val operationDate: LocalDate,
    ) : OperationSuggestion()

    data class NewCategory(
        val type: CategoryType,
        val amount: Double,
        val categoryName: String,
        val categoryIcon: String,
        val description: String?,
        val operationDate: LocalDate,
    ) : OperationSuggestion()

    data class AmbiguousCategory(
        val type: CategoryType,
        val amount: Double,
        val categoryName: String,
        val description: String?,
        val operationDate: LocalDate,
    ) : OperationSuggestion()

    data class CategoryMissing(
        val type: CategoryType,
        val amount: Double,
        val description: String?,
        val operationDate: LocalDate,
    ) : OperationSuggestion()

    object AmountNotPositive : OperationSuggestion()
}
