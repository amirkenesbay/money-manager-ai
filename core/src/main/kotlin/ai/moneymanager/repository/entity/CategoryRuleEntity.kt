package ai.moneymanager.repository.entity

import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "category_rule")
@CompoundIndex(def = "{'groupId': 1, 'merchantKey': 1}", unique = true)
data class CategoryRuleEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val merchantKey: String,
    val categoryId: ObjectId,
    val auditInfo: AuditInfo = AuditInfo(),
)
