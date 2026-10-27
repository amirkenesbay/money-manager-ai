package ai.moneymanager.web.notification

import ai.moneymanager.domain.model.MAX_NOTIFICATION_NAME_LENGTH
import ai.moneymanager.domain.model.ScheduleAnchor
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.input.normalizeIcon
import ai.moneymanager.web.input.requireName

private const val INVALID_SCHEDULE_KEY = "${API_ERROR_KEY_PREFIX}invalid_schedule"
private val HOURS = 0..23
private val MINUTES = 0..59
private val DAYS_OF_MONTH = 1..31
private val MONTHS = 1..12
private const val MIN_CUSTOM_INTERVAL = 1

fun notificationDraft(request: NotificationRequest): NotificationDraft = NotificationDraft(
    name = requireName(request.name, MAX_NOTIFICATION_NAME_LENGTH),
    icon = normalizeIcon(request.icon),
    schedule = scheduleOf(request),
)

private fun scheduleOf(request: NotificationRequest): NotificationSchedule {
    val type = request.frequencyType
    val anchor = type.anchor
    val dayOfMonthNeeded = anchor == ScheduleAnchor.DAY_OF_MONTH || anchor == ScheduleAnchor.DAY_OF_YEAR
    return NotificationSchedule(
        frequencyType = type,
        customInterval = type.maxCustomInterval?.let { max -> require(request.customInterval, MIN_CUSTOM_INTERVAL..max) },
        hour = require(request.hour, HOURS),
        minute = require(request.minute, MINUTES),
        dayOfWeek = if (anchor == ScheduleAnchor.DAY_OF_WEEK) request.dayOfWeek ?: throw invalidSchedule() else null,
        dayOfMonth = if (dayOfMonthNeeded) require(request.dayOfMonth, DAYS_OF_MONTH) else null,
        monthOfYear = if (anchor == ScheduleAnchor.DAY_OF_YEAR) require(request.monthOfYear, MONTHS) else null,
    )
}

private fun require(value: Int?, range: IntRange): Int = value?.takeIf { it in range } ?: throw invalidSchedule()

private fun invalidSchedule() = badRequest(INVALID_SCHEDULE_KEY)
