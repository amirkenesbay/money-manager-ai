package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

@Document(collection = "recurring_payment")
@CompoundIndexes(
    CompoundIndex(def = "{'groupId': 1}"),
    CompoundIndex(def = "{'active': 1, 'nextDate': 1}"),
)
data class RecurringPaymentEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val creatorId: Long,
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val categoryName: String,
    val categoryIcon: String?,
    val description: String?,
    val unit: RecurrenceUnit,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val nextDate: LocalDate,
    val mode: RecurringMode,
    val active: Boolean = true,
    val headsUpSentFor: LocalDate? = null,
    val auditInfo: AuditInfo = AuditInfo(),
) {
    val recurrence: Recurrence
        get() = Recurrence(unit, interval)

    val finished: Boolean
        get() = endDate != null && nextDate.isAfter(endDate)
}
