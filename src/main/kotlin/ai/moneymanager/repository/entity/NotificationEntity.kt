package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.DayOfWeek
import java.time.LocalDateTime

@Document(collection = "notification")
@CompoundIndexes(
    CompoundIndex(def = "{'telegramUserId': 1}"),
    CompoundIndex(def = "{'isActive': 1, 'nextFireTime': 1}")
)
data class NotificationEntity(
    val id: ObjectId? = null,
    val telegramUserId: Long,
    val name: String,
    val icon: String? = null,
    val frequencyType: FrequencyType,
    val customInterval: Int? = null,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: DayOfWeek? = null,
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    val isActive: Boolean = true,
    val nextFireTime: LocalDateTime,
    val auditInfo: AuditInfo = AuditInfo()
)
