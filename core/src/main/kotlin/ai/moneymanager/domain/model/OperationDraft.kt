package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class OperationDraft(
    val type: CategoryType,
    val amount: BigDecimal,
    val category: Category,
    val operationDate: LocalDate,
    val description: String?,
)
