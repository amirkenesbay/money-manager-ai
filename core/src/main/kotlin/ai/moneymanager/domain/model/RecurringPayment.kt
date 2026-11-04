package ai.moneymanager.domain.model

import ai.moneymanager.repository.entity.RecurringPaymentEntity
import java.math.BigDecimal
import java.time.LocalDate

enum class RecurringMode {
    AUTO,
    REMIND,
}

data class RecurringDraft(
    val type: CategoryType,
    val amount: BigDecimal,
    val category: Category,
    val description: String?,
    val recurrence: Recurrence,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val mode: RecurringMode,
)

sealed interface RecurringSaveResult {
    data class Saved(val payment: RecurringPaymentEntity) : RecurringSaveResult
    data class LimitReached(val limit: Int) : RecurringSaveResult
    data object NotFound : RecurringSaveResult
}
