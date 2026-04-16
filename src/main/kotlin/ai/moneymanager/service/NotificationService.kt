package ai.moneymanager.service

import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.repository.NotificationRepository
import ai.moneymanager.repository.entity.NotificationEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    fun createNotification(
        telegramUserId: Long,
        name: String,
        icon: String?,
        frequencyType: FrequencyType,
        customInterval: Int?,
        hour: Int,
        minute: Int,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?,
        monthOfYear: Int?,
        userTimezone: String
    ): NotificationEntity {
        val nextFireTime = computeNextFireTime(
            frequencyType, customInterval, hour, minute,
            dayOfWeek, dayOfMonth, monthOfYear, userTimezone
        )

        val entity = NotificationEntity(
            telegramUserId = telegramUserId,
            name = name,
            icon = icon,
            frequencyType = frequencyType,
            customInterval = customInterval,
            hour = hour,
            minute = minute,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            monthOfYear = monthOfYear,
            nextFireTime = nextFireTime
        )

        val saved = notificationRepository.save(entity)
        log.info("Created notification '{}' for user {}, nextFireTime={}", name, telegramUserId, nextFireTime)
        return saved
    }

    fun getNotifications(telegramUserId: Long): List<NotificationEntity> =
        notificationRepository.findByTelegramUserIdOrderByAuditInfoCreatedAtAsc(telegramUserId)

    fun getNotification(id: ObjectId): NotificationEntity? =
        notificationRepository.findById(id).orElse(null)

    fun toggleNotification(id: ObjectId, userTimezone: String): NotificationEntity? {
        val entity = notificationRepository.findById(id).orElse(null) ?: return null
        val toggled = entity.copy(isActive = !entity.isActive)

        val updated = if (toggled.isActive) {
            val newFireTime = computeNextFireTime(
                toggled.frequencyType, toggled.customInterval, toggled.hour, toggled.minute,
                toggled.dayOfWeek, toggled.dayOfMonth, toggled.monthOfYear, userTimezone
            )
            toggled.copy(nextFireTime = newFireTime)
        } else {
            toggled
        }

        return notificationRepository.save(updated)
    }

    fun updateName(id: ObjectId, newName: String): NotificationEntity? {
        val entity = notificationRepository.findById(id).orElse(null) ?: return null
        return notificationRepository.save(entity.copy(name = newName))
    }

    fun updateIcon(id: ObjectId, newIcon: String?): NotificationEntity? {
        val entity = notificationRepository.findById(id).orElse(null) ?: return null
        return notificationRepository.save(entity.copy(icon = newIcon))
    }

    fun updateFrequency(
        id: ObjectId,
        frequencyType: FrequencyType,
        customInterval: Int?,
        hour: Int,
        minute: Int,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?,
        monthOfYear: Int?,
        userTimezone: String
    ): NotificationEntity? {
        val entity = notificationRepository.findById(id).orElse(null) ?: return null
        val nextFireTime = computeNextFireTime(
            frequencyType, customInterval, hour, minute,
            dayOfWeek, dayOfMonth, monthOfYear, userTimezone
        )
        return notificationRepository.save(
            entity.copy(
                frequencyType = frequencyType,
                customInterval = customInterval,
                hour = hour,
                minute = minute,
                dayOfWeek = dayOfWeek,
                dayOfMonth = dayOfMonth,
                monthOfYear = monthOfYear,
                nextFireTime = nextFireTime
            )
        )
    }

    fun deleteNotification(id: ObjectId): Boolean {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id)
            return true
        }
        return false
    }

    fun deleteAllNotifications(telegramUserId: Long): Long =
        notificationRepository.deleteByTelegramUserId(telegramUserId)

    fun getDueNotifications(now: LocalDateTime): List<NotificationEntity> =
        notificationRepository.findByIsActiveTrueAndNextFireTimeLessThanEqual(now)

    fun advanceNextFireTime(notification: NotificationEntity, userTimezone: String): NotificationEntity {
        val nextFireTime = computeNextFireTime(
            notification.frequencyType, notification.customInterval,
            notification.hour, notification.minute,
            notification.dayOfWeek, notification.dayOfMonth, notification.monthOfYear,
            userTimezone, fromTime = notification.nextFireTime
        )
        return notificationRepository.save(notification.copy(nextFireTime = nextFireTime))
    }

    internal fun computeNextFireTime(
        frequencyType: FrequencyType,
        customInterval: Int?,
        hour: Int,
        minute: Int,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?,
        monthOfYear: Int?,
        userTimezone: String,
        fromTime: LocalDateTime? = null
    ): LocalDateTime {
        val zone = ZoneId.of(userTimezone)
        val now = if (fromTime != null) {
            fromTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone).toLocalDateTime()
        } else {
            ZonedDateTime.now(zone).toLocalDateTime()
        }

        val time = LocalTime.of(hour, minute)
        val interval = customInterval ?: 1

        val nextLocal = when (frequencyType) {
            FrequencyType.DAILY -> nextDaily(now, time)
            FrequencyType.CUSTOM_DAYS -> nextCustomDays(now, time, interval)

            FrequencyType.WEEKLY -> nextWeekly(now, time, dayOfWeek!!, 1)
            FrequencyType.BIWEEKLY -> nextWeekly(now, time, dayOfWeek!!, 2)
            FrequencyType.CUSTOM_WEEKS -> nextWeekly(now, time, dayOfWeek!!, interval)

            FrequencyType.MONTHLY -> nextMonthly(now, time, dayOfMonth!!, 1)
            FrequencyType.BIMONTHLY -> nextMonthly(now, time, dayOfMonth!!, 2)
            FrequencyType.CUSTOM_MONTHS -> nextMonthly(now, time, dayOfMonth!!, interval)

            FrequencyType.YEARLY -> nextYearly(now, time, monthOfYear!!, dayOfMonth!!, 1)
            FrequencyType.CUSTOM_YEARS -> nextYearly(now, time, monthOfYear!!, dayOfMonth!!, interval)
        }

        return nextLocal.atZone(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    private fun nextDaily(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun nextCustomDays(now: LocalDateTime, time: LocalTime, interval: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(interval.toLong())
    }

    private fun nextWeekly(now: LocalDateTime, time: LocalTime, dow: DayOfWeek, weeksInterval: Int): LocalDateTime {
        var candidate = now.toLocalDate().atTime(time)

        while (candidate.dayOfWeek != dow) {
            candidate = candidate.plusDays(1)
        }

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(weeksInterval.toLong())
        }

        return candidate
    }

    private fun nextMonthly(now: LocalDateTime, time: LocalTime, dayOfMonth: Int, monthsInterval: Int): LocalDateTime {
        var date = now.toLocalDate()
        val clampedDay = minOf(dayOfMonth, YearMonth.of(date.year, date.month).lengthOfMonth())
        var candidate = date.withDayOfMonth(clampedDay).atTime(time)

        if (!candidate.isAfter(now)) {
            date = date.plusMonths(monthsInterval.toLong())
            val newClampedDay = minOf(dayOfMonth, YearMonth.of(date.year, date.month).lengthOfMonth())
            candidate = date.withDayOfMonth(newClampedDay).atTime(time)
        }

        return candidate
    }

    private fun nextYearly(
        now: LocalDateTime,
        time: LocalTime,
        month: Int,
        dayOfMonth: Int,
        yearsInterval: Int
    ): LocalDateTime {
        var year = now.year
        val clampedDay = minOf(dayOfMonth, YearMonth.of(year, month).lengthOfMonth())
        var candidate = LocalDateTime.of(year, month, clampedDay, time.hour, time.minute)

        if (!candidate.isAfter(now)) {
            year += yearsInterval
            val newClampedDay = minOf(dayOfMonth, YearMonth.of(year, month).lengthOfMonth())
            candidate = LocalDateTime.of(year, month, newClampedDay, time.hour, time.minute)
        }

        return candidate
    }
}
