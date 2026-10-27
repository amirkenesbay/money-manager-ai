package ai.moneymanager.web.notification

import ai.moneymanager.domain.model.DEFAULT_NOTIFICATION_ICON
import ai.moneymanager.domain.model.NotificationCreationResult
import ai.moneymanager.domain.model.NotificationToggleResult
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.error.limitReached
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId

private const val NOTIFICATION_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}notification_not_found"
private const val TIMEZONE_REQUIRED_KEY = "${API_ERROR_KEY_PREFIX}timezone_required"

@RestController
@RequestMapping("$API_V1_PREFIX/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
) {
    @GetMapping
    fun notifications(user: UserInfo): List<NotificationResponse> =
        notificationService.getNotifications(checkNotNull(user.telegramUserId)).map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @RequestBody request: NotificationRequest): NotificationResponse {
        val draft = notificationDraft(request)
        val schedule = draft.schedule
        val result = notificationService.createNotification(
            telegramUserId = checkNotNull(user.telegramUserId),
            name = draft.name,
            icon = draft.icon,
            frequencyType = schedule.frequencyType,
            customInterval = schedule.customInterval,
            hour = schedule.hour,
            minute = schedule.minute,
            dayOfWeek = schedule.dayOfWeek,
            dayOfMonth = schedule.dayOfMonth,
            monthOfYear = schedule.monthOfYear,
            userTimezone = timezoneOf(user),
            maxActiveNotifications = maxActive(user),
        )
        return when (result) {
            is NotificationCreationResult.Created -> result.notification.toResponse()
            is NotificationCreationResult.LimitReached -> throw limitReached(ApiErrorCode.NOTIFICATION_LIMIT_REACHED, result.limit)
        }
    }

    @PutMapping("/{notificationId}")
    fun replace(user: UserInfo, @PathVariable notificationId: ObjectId, @RequestBody request: NotificationRequest): NotificationResponse {
        requireOwn(user, notificationId)
        val draft = notificationDraft(request)
        val schedule = draft.schedule
        notificationService.updateName(notificationId, draft.name)
        notificationService.updateIcon(notificationId, draft.icon)
        val updated = notificationService.updateFrequency(
            id = notificationId,
            frequencyType = schedule.frequencyType,
            customInterval = schedule.customInterval,
            hour = schedule.hour,
            minute = schedule.minute,
            dayOfWeek = schedule.dayOfWeek,
            dayOfMonth = schedule.dayOfMonth,
            monthOfYear = schedule.monthOfYear,
            userTimezone = timezoneOf(user),
        ) ?: throw notFound()
        return updated.toResponse()
    }

    @PatchMapping("/{notificationId}")
    fun setActive(user: UserInfo, @PathVariable notificationId: ObjectId, @RequestBody request: NotificationActivityRequest): NotificationResponse {
        requireOwn(user, notificationId)
        return when (val result = notificationService.setActive(notificationId, request.isActive, timezoneOf(user), maxActive(user))) {
            is NotificationToggleResult.Updated -> result.notification.toResponse()
            is NotificationToggleResult.LimitReached -> throw limitReached(ApiErrorCode.NOTIFICATION_LIMIT_REACHED, result.limit)
            NotificationToggleResult.NotFound -> throw notFound()
        }
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable notificationId: ObjectId) {
        requireOwn(user, notificationId)
        notificationService.deleteNotification(notificationId)
    }

    private fun requireOwn(user: UserInfo, notificationId: ObjectId): NotificationEntity =
        notificationService.getNotification(notificationId)?.takeIf { it.telegramUserId == user.telegramUserId } ?: throw notFound()

    private fun timezoneOf(user: UserInfo): String = user.timezone ?: throw badRequest(TIMEZONE_REQUIRED_KEY)

    private fun maxActive(user: UserInfo): Int? = subscriptionLimitsService.maxActiveNotifications(user.hasActivePaidSubscription())

    private fun notFound() = ApiException(ApiErrorCode.NOT_FOUND, NOTIFICATION_NOT_FOUND_KEY)

    private fun NotificationEntity.toResponse() = NotificationResponse(
        id = checkNotNull(id),
        name = name,
        icon = icon ?: DEFAULT_NOTIFICATION_ICON,
        isActive = isActive,
        frequencyType = frequencyType,
        customInterval = customInterval,
        hour = hour,
        minute = minute,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        nextFireTime = nextFireTime.atZone(ZoneId.systemDefault()).toInstant(),
    )
}
