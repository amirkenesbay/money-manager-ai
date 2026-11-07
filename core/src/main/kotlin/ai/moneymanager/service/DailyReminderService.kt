package ai.moneymanager.service

import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.model.FALLBACK_TIMEZONE
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.NotificationCreationResult
import ai.moneymanager.domain.model.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

private const val REMINDER_NAME_KEY = "notification.default.daily_spending"
private const val REMINDER_ICON = "💸"
private const val REMINDER_HOUR = 21
private const val REMINDER_MINUTE = 0

@Service
class DailyReminderService(
    private val userInfoService: UserInfoService,
    private val notificationService: NotificationService,
    private val localizationService: LocalizationService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun remindEveryone() {
        val created = userInfoService.usersWithoutDailyReminder().count { ensureFor(it) }
        if (created > 0) log.info("Created a daily spending reminder for {} users", created)
    }

    @EventListener
    fun onNewUserRegistered(event: NewUserRegistered) {
        ensureFor(event.user)
    }

    fun ensureFor(user: UserInfo): Boolean {
        val telegramUserId = user.telegramUserId ?: return false
        if (user.dailyReminderSeeded || user.blocked) return false
        val hasDaily = notificationService.getNotifications(telegramUserId).any { it.isActive && it.frequencyType == FrequencyType.DAILY }
        val created = !hasDaily && create(user, telegramUserId)
        userInfoService.markDailyReminderSeeded(telegramUserId)
        return created
    }

    private fun create(user: UserInfo, telegramUserId: Long): Boolean {
        val language = localizationService.resolveLanguage(user.language, user.languageCode)
        val result = notificationService.createNotification(
            telegramUserId = telegramUserId,
            name = localizationService.t(REMINDER_NAME_KEY, language),
            icon = REMINDER_ICON,
            frequencyType = FrequencyType.DAILY,
            customInterval = null,
            hour = REMINDER_HOUR,
            minute = REMINDER_MINUTE,
            dayOfWeek = null,
            dayOfMonth = null,
            monthOfYear = null,
            userTimezone = user.timezone ?: FALLBACK_TIMEZONE,
            maxActiveNotifications = subscriptionLimitsService.maxActiveNotifications(user.hasActivePaidSubscription()),
        )
        return result is NotificationCreationResult.Created
    }
}
