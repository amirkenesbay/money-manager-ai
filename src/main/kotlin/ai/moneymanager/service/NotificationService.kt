package ai.moneymanager.service

import ai.moneymanager.domain.model.Notification
import ai.moneymanager.domain.model.NotificationCustomUnit
import ai.moneymanager.domain.model.NotificationPeriodType
import ai.moneymanager.repository.NotificationRepository
import ai.moneymanager.repository.entity.NotificationEntity
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    /**
     * Создать новое уведомление
     */
    fun createNotification(
        userId: Long,
        title: String,
        periodType: NotificationPeriodType,
        hour: Int,
        minute: Int,
        customN: Int? = null,
        customUnit: NotificationCustomUnit? = null,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        month: Int? = null
    ): Notification {
        val nextTriggerTime = calculateNextTriggerTime(
            periodType = periodType,
            hour = hour,
            minute = minute,
            customN = customN,
            customUnit = customUnit,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            month = month,
            fromTime = LocalDateTime.now()
        )

        val entity = NotificationEntity(
            userId = userId,
            title = title,
            periodType = periodType,
            customN = customN,
            customUnit = customUnit,
            hour = hour,
            minute = minute,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            month = month,
            nextTriggerTime = nextTriggerTime
        )

        val saved = notificationRepository.save(entity)
        return mapToModel(saved)
    }

    /**
     * Получить все уведомления пользователя
     */
    fun getNotificationsByUserId(userId: Long): List<Notification> {
        return notificationRepository.findByUserId(userId)
            .map { mapToModel(it) }
    }

    /**
     * Получить уведомление по ID
     */
    fun getNotification(notificationId: ObjectId): Notification? {
        return notificationRepository.findById(notificationId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    /**
     * Обновить название уведомления
     */
    fun updateNotificationTitle(notificationId: ObjectId, newTitle: String): Notification? {
        val entity = notificationRepository.findById(notificationId).orElse(null) ?: return null
        val updated = entity.copy(title = newTitle)
        val saved = notificationRepository.save(updated)
        return mapToModel(saved)
    }

    /**
     * Обновить периодичность уведомления
     */
    fun updateNotificationPeriod(
        notificationId: ObjectId,
        periodType: NotificationPeriodType,
        hour: Int,
        minute: Int,
        customN: Int? = null,
        customUnit: NotificationCustomUnit? = null,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        month: Int? = null
    ): Notification? {
        val entity = notificationRepository.findById(notificationId).orElse(null) ?: return null

        val nextTriggerTime = calculateNextTriggerTime(
            periodType = periodType,
            hour = hour,
            minute = minute,
            customN = customN,
            customUnit = customUnit,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            month = month,
            fromTime = LocalDateTime.now()
        )

        val updated = entity.copy(
            periodType = periodType,
            customN = customN,
            customUnit = customUnit,
            hour = hour,
            minute = minute,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            month = month,
            nextTriggerTime = nextTriggerTime
        )
        val saved = notificationRepository.save(updated)
        return mapToModel(saved)
    }

    /**
     * Удалить уведомление
     */
    fun deleteNotification(notificationId: ObjectId): Boolean {
        return try {
            notificationRepository.deleteById(notificationId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Удалить все уведомления пользователя
     */
    fun deleteAllNotificationsForUser(userId: Long): Int {
        return try {
            val deletedCount = notificationRepository.deleteByUserId(userId)
            deletedCount.toInt()
        } catch (e: Exception) {
            println("❌ Error deleting all notifications for user $userId: ${e.message}")
            0
        }
    }

    /**
     * Получить уведомления, которые должны сработать
     */
    fun getNotificationsToTrigger(): List<Notification> {
        val now = LocalDateTime.now()
        return notificationRepository.findByNextTriggerTimeBefore(now)
            .map { mapToModel(it) }
    }

    /**
     * Обновить nextTriggerTime после срабатывания
     */
    fun updateNextTriggerTime(notificationId: ObjectId): Notification? {
        val entity = notificationRepository.findById(notificationId).orElse(null) ?: return null

        val nextTriggerTime = calculateNextTriggerTime(
            periodType = entity.periodType,
            hour = entity.hour,
            minute = entity.minute,
            customN = entity.customN,
            customUnit = entity.customUnit,
            dayOfWeek = entity.dayOfWeek,
            dayOfMonth = entity.dayOfMonth,
            month = entity.month,
            fromTime = LocalDateTime.now()
        )

        val updated = entity.copy(nextTriggerTime = nextTriggerTime)
        val saved = notificationRepository.save(updated)
        return mapToModel(saved)
    }

    /**
     * Рассчитать следующее время срабатывания
     */
    fun calculateNextTriggerTime(
        periodType: NotificationPeriodType,
        hour: Int,
        minute: Int,
        customN: Int?,
        customUnit: NotificationCustomUnit?,
        dayOfWeek: Int?,
        dayOfMonth: Int?,
        month: Int?,
        fromTime: LocalDateTime
    ): LocalDateTime {
        val targetTime = LocalTime.of(hour, minute)
        val now = fromTime

        return when (periodType) {
            NotificationPeriodType.DAILY -> {
                val todayAtTime = now.toLocalDate().atTime(targetTime)
                if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
            }

            NotificationPeriodType.WEEKLY -> {
                calculateWeeklyTrigger(now, dayOfWeek ?: 1, targetTime, 1)
            }

            NotificationPeriodType.BIWEEKLY -> {
                calculateWeeklyTrigger(now, dayOfWeek ?: 1, targetTime, 2)
            }

            NotificationPeriodType.MONTHLY -> {
                calculateMonthlyTrigger(now, dayOfMonth ?: 1, targetTime, 1)
            }

            NotificationPeriodType.BIMONTHLY -> {
                calculateMonthlyTrigger(now, dayOfMonth ?: 1, targetTime, 2)
            }

            NotificationPeriodType.YEARLY -> {
                calculateYearlyTrigger(now, month ?: 1, dayOfMonth ?: 1, targetTime)
            }

            NotificationPeriodType.CUSTOM -> {
                calculateCustomTrigger(now, customN ?: 1, customUnit ?: NotificationCustomUnit.DAYS, targetTime, dayOfWeek, dayOfMonth, month)
            }
        }
    }

    private fun calculateWeeklyTrigger(
        now: LocalDateTime,
        dayOfWeek: Int,
        targetTime: LocalTime,
        weekMultiplier: Int
    ): LocalDateTime {
        val targetDayOfWeek = DayOfWeek.of(dayOfWeek)
        var nextDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
        var nextDateTime = nextDate.atTime(targetTime)

        if (!nextDateTime.isAfter(now)) {
            nextDate = nextDate.plusWeeks(weekMultiplier.toLong())
            nextDateTime = nextDate.atTime(targetTime)
        }

        return nextDateTime
    }

    private fun calculateMonthlyTrigger(
        now: LocalDateTime,
        dayOfMonth: Int,
        targetTime: LocalTime,
        monthMultiplier: Int
    ): LocalDateTime {
        val safeDay = dayOfMonth.coerceIn(1, 28)
        var nextDate = now.toLocalDate().withDayOfMonth(safeDay)
        var nextDateTime = nextDate.atTime(targetTime)

        if (!nextDateTime.isAfter(now)) {
            nextDate = nextDate.plusMonths(monthMultiplier.toLong())
            nextDateTime = nextDate.atTime(targetTime)
        }

        return nextDateTime
    }

    private fun calculateYearlyTrigger(
        now: LocalDateTime,
        month: Int,
        dayOfMonth: Int,
        targetTime: LocalTime
    ): LocalDateTime {
        val safeDay = dayOfMonth.coerceIn(1, 28)
        var nextDate = now.toLocalDate().withMonth(month).withDayOfMonth(safeDay)
        var nextDateTime = nextDate.atTime(targetTime)

        if (!nextDateTime.isAfter(now)) {
            nextDate = nextDate.plusYears(1)
            nextDateTime = nextDate.atTime(targetTime)
        }

        return nextDateTime
    }

    private fun calculateCustomTrigger(
        now: LocalDateTime,
        n: Int,
        unit: NotificationCustomUnit,
        targetTime: LocalTime,
        dayOfWeek: Int?,
        dayOfMonth: Int?,
        month: Int?
    ): LocalDateTime {
        return when (unit) {
            NotificationCustomUnit.DAYS -> {
                val todayAtTime = now.toLocalDate().atTime(targetTime)
                if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(n.toLong())
            }
            NotificationCustomUnit.WEEKS -> {
                calculateWeeklyTrigger(now, dayOfWeek ?: 1, targetTime, n)
            }
            NotificationCustomUnit.MONTHS -> {
                calculateMonthlyTrigger(now, dayOfMonth ?: 1, targetTime, n)
            }
            NotificationCustomUnit.YEARS -> {
                calculateYearlyTrigger(now, month ?: 1, dayOfMonth ?: 1, targetTime)
            }
        }
    }

    /**
     * Форматировать периодичность в читаемую строку
     */
    fun formatPeriodicity(notification: Notification): String {
        val timeStr = String.format("%02d:%02d", notification.hour, notification.minute)

        return when (notification.periodType) {
            NotificationPeriodType.DAILY -> "каждый день в $timeStr"
            NotificationPeriodType.WEEKLY -> {
                val dayName = getDayOfWeekName(notification.dayOfWeek ?: 1)
                "каждую неделю в $dayName в $timeStr"
            }
            NotificationPeriodType.BIWEEKLY -> {
                val dayName = getDayOfWeekName(notification.dayOfWeek ?: 1)
                "каждые 2 недели в $dayName в $timeStr"
            }
            NotificationPeriodType.MONTHLY -> {
                "каждый месяц ${notification.dayOfMonth ?: 1} числа в $timeStr"
            }
            NotificationPeriodType.BIMONTHLY -> {
                "каждые 2 месяца ${notification.dayOfMonth ?: 1} числа в $timeStr"
            }
            NotificationPeriodType.YEARLY -> {
                val monthName = getMonthName(notification.month ?: 1)
                "${notification.dayOfMonth ?: 1} $monthName в $timeStr"
            }
            NotificationPeriodType.CUSTOM -> {
                formatCustomPeriodicity(notification, timeStr)
            }
        }
    }

    private fun formatCustomPeriodicity(notification: Notification, timeStr: String): String {
        val n = notification.customN ?: 1
        val unit = notification.customUnit ?: NotificationCustomUnit.DAYS

        return when (unit) {
            NotificationCustomUnit.DAYS -> "каждые $n ${pluralizeDays(n)} в $timeStr"
            NotificationCustomUnit.WEEKS -> {
                val dayName = getDayOfWeekName(notification.dayOfWeek ?: 1)
                "каждые $n ${pluralizeWeeks(n)} в $dayName в $timeStr"
            }
            NotificationCustomUnit.MONTHS -> {
                "каждые $n ${pluralizeMonths(n)} ${notification.dayOfMonth ?: 1} числа в $timeStr"
            }
            NotificationCustomUnit.YEARS -> {
                val monthName = getMonthName(notification.month ?: 1)
                "каждые $n ${pluralizeYears(n)} ${notification.dayOfMonth ?: 1} $monthName в $timeStr"
            }
        }
    }

    private fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "понедельник"
            2 -> "вторник"
            3 -> "среду"
            4 -> "четверг"
            5 -> "пятницу"
            6 -> "субботу"
            7 -> "воскресенье"
            else -> "понедельник"
        }
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "января"
            2 -> "февраля"
            3 -> "марта"
            4 -> "апреля"
            5 -> "мая"
            6 -> "июня"
            7 -> "июля"
            8 -> "августа"
            9 -> "сентября"
            10 -> "октября"
            11 -> "ноября"
            12 -> "декабря"
            else -> "января"
        }
    }

    private fun pluralizeDays(n: Int): String {
        return when {
            n % 100 in 11..19 -> "дней"
            n % 10 == 1 -> "день"
            n % 10 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    private fun pluralizeWeeks(n: Int): String {
        return when {
            n % 100 in 11..19 -> "недель"
            n % 10 == 1 -> "неделю"
            n % 10 in 2..4 -> "недели"
            else -> "недель"
        }
    }

    private fun pluralizeMonths(n: Int): String {
        return when {
            n % 100 in 11..19 -> "месяцев"
            n % 10 == 1 -> "месяц"
            n % 10 in 2..4 -> "месяца"
            else -> "месяцев"
        }
    }

    private fun pluralizeYears(n: Int): String {
        return when {
            n % 100 in 11..19 -> "лет"
            n % 10 == 1 -> "год"
            n % 10 in 2..4 -> "года"
            else -> "лет"
        }
    }

    /**
     * Маппинг Entity -> Model
     */
    private fun mapToModel(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id,
            userId = entity.userId,
            title = entity.title,
            periodType = entity.periodType,
            customN = entity.customN,
            customUnit = entity.customUnit,
            hour = entity.hour,
            minute = entity.minute,
            dayOfWeek = entity.dayOfWeek,
            dayOfMonth = entity.dayOfMonth,
            month = entity.month,
            nextTriggerTime = entity.nextTriggerTime,
            createdAt = entity.auditInfo.createdAt,
            updatedAt = entity.auditInfo.modifiedAt
        )
    }
}