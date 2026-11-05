package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

data class LoanRepayment(
    val id: ObjectId = ObjectId(),
    val amount: BigDecimal,
    val date: LocalDate,
)

@Document(collection = "loan")
@CompoundIndex(def = "{'groupId': 1, 'creatorId': 1}")
data class LoanEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val creatorId: Long,
    val direction: LoanDirection,
    val person: String,
    val amount: BigDecimal,
    val lentOn: LocalDate,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val repayments: List<LoanRepayment> = emptyList(),
    val dueRemindedOn: LocalDate? = null,
    val auditInfo: AuditInfo = AuditInfo(),
) {
    val repaid: BigDecimal
        get() = repayments.fold(BigDecimal.ZERO) { total, repayment -> total + repayment.amount }

    val remaining: BigDecimal
        get() = (amount - repaid).max(BigDecimal.ZERO)

    val open: Boolean
        get() = remaining.signum() > 0

    val cashEffect: BigDecimal
        get() = if (direction == LoanDirection.BORROWED) remaining else remaining.negate()
}
