package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.NotificationCustomUnit
import ai.moneymanager.domain.model.NotificationPeriodType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "notification")
data class NotificationEntity(
    val id: ObjectId? = null,
    @Indexed
    val userId: Long,
    val title: String,
    val periodType: NotificationPeriodType,
    val customN: Int? = null,
    val customUnit: NotificationCustomUnit? = null,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val month: Int? = null,
    @Indexed
    val nextTriggerTime: LocalDateTime,
    val auditInfo: AuditInfo = AuditInfo()
)