package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "category")
@CompoundIndex(def = "{'groupId': 1, 'name': 1}", unique = true)
data class CategoryEntity(
    val id: ObjectId? = null,
    val name: String,
    val icon: String? = null,
    val type: CategoryType,
    val groupId: ObjectId,
    val auditInfo: AuditInfo = AuditInfo()
)