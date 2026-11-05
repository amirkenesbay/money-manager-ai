package ai.moneymanager.domain.model.loan

import java.math.BigDecimal
import java.time.LocalDate

enum class LoanDirection {
    LENT,
    BORROWED,
}

data class LoanDraft(
    val direction: LoanDirection,
    val person: String,
    val amount: BigDecimal,
    val date: LocalDate,
    val dueDate: LocalDate?,
    val note: String?,
) {
    init {
        require(amount.signum() > 0) { "A loan has a positive amount" }
        require(person.isNotBlank()) { "A loan has a person" }
    }
}
