package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

@Document(collection = "finance_operation")
@CompoundIndexes(
    CompoundIndex(def = "{'groupId': 1, 'operationDate': -1}"),
    CompoundIndex(def = "{'groupId': 1, 'type': 1, 'operationDate': -1}"),
    CompoundIndex(def = "{'groupId': 1, 'categoryId': 1, 'operationDate': -1}")
)
data class FinanceOperationEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val creatorId: Long,
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val categoryName: String,
    val categoryIcon: String? = null,
    val operationDate: LocalDate,
    val description: String? = null,
    val auditInfo: AuditInfo = AuditInfo()
)
