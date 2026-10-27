package ai.moneymanager.web.notification

import ai.moneymanager.domain.model.FrequencyType
import org.bson.types.ObjectId
import java.time.DayOfWeek
import java.time.Instant

data class NotificationResponse(
    val id: ObjectId,
    val name: String,
    val icon: String,
    val isActive: Boolean,
    val frequencyType: FrequencyType,
    val customInterval: Int?,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: DayOfWeek?,
    val dayOfMonth: Int?,
    val monthOfYear: Int?,
    val nextFireTime: Instant,
)

data class NotificationRequest(
    val name: String?,
    val icon: String?,
    val frequencyType: FrequencyType,
    val customInterval: Int?,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: DayOfWeek?,
    val dayOfMonth: Int?,
    val monthOfYear: Int?,
)

data class NotificationActivityRequest(
    val isActive: Boolean,
)

data class NotificationSchedule(
    val frequencyType: FrequencyType,
    val customInterval: Int?,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: DayOfWeek?,
    val dayOfMonth: Int?,
    val monthOfYear: Int?,
)

data class NotificationDraft(
    val name: String,
    val icon: String?,
    val schedule: NotificationSchedule,
)
