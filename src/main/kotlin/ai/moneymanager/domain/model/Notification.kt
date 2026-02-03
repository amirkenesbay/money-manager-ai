package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.time.LocalDateTime

/**
 * Уведомление (напоминание) для пользователя
 */
data class Notification(
    val id: ObjectId? = null,
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
    val nextTriggerTime: LocalDateTime,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

/**
 * Тип периодичности уведомления
 */
enum class NotificationPeriodType {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    BIMONTHLY,
    YEARLY,
    CUSTOM
}

/**
 * Единица измерения для пользовательской периодичности
 */
enum class NotificationCustomUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS
}
