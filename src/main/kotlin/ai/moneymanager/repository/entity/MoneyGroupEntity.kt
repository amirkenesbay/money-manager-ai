package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "money_group")
data class MoneyGroupEntity(
    val id: String,
    val name: String,
    @Indexed(unique = true)
    val inviteToken: String,
    val ownerId: Long,
    val memberIds: Set<Long>,
    val type: GroupType,
    val auditInfo: AuditInfo = AuditInfo()
)