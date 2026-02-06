package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "money_group")
data class MoneyGroupEntity(
    @Id
    val id: ObjectId? = null,
    val name: String,

    @Indexed(unique = true)
    val inviteToken: String,

    val ownerTelegramUserId: Long,

    @Indexed
    val memberTelegramUserIds: Set<Long>,

    val type: GroupType,
    val defaultCurrency: CurrencyType? = null,
    val auditInfo: AuditInfo = AuditInfo()
)