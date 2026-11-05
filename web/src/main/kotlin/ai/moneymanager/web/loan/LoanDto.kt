package ai.moneymanager.web.loan

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.repository.entity.LoanEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

data class LoanRequest(
    val direction: LoanDirection,
    val person: String?,
    val amount: BigDecimal,
    val date: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val note: String? = null,
)

data class RepaymentRequest(
    val amount: BigDecimal? = null,
)

data class RepaymentResponse(
    val id: ObjectId,
    val amount: BigDecimal,
    val date: LocalDate,
)

data class LoanResponse(
    val id: ObjectId,
    val direction: LoanDirection,
    val person: String,
    val amount: BigDecimal,
    val repaid: BigDecimal,
    val remaining: BigDecimal,
    val lentOn: LocalDate,
    val dueDate: LocalDate?,
    val note: String?,
    val repayments: List<RepaymentResponse>,
    val open: Boolean,
)

fun LoanEntity.toResponse() = LoanResponse(
    id = checkNotNull(id),
    direction = direction,
    person = person,
    amount = amount,
    repaid = repaid,
    remaining = remaining,
    lentOn = lentOn,
    dueDate = dueDate,
    note = note,
    repayments = repayments.sortedByDescending { it.date }.map { RepaymentResponse(it.id, it.amount, it.date) },
    open = open,
)
