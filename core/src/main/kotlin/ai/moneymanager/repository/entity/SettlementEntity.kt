package ai.moneymanager.repository.entity

import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

@Document(collection = "settlement")
@CompoundIndex(def = "{'groupId': 1, 'settledOn': -1}")
data class SettlementEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val fromId: Long,
    val toId: Long,
    val amount: BigDecimal,
    val settledOn: LocalDate,
    val creatorId: Long,
    val auditInfo: AuditInfo = AuditInfo(),
)
