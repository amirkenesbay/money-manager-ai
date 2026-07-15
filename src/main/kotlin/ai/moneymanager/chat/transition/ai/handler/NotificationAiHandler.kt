package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatTime
import ai.moneymanager.chat.reply.notification.formatFrequencyShort
import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.NotificationService
import org.springframework.stereotype.Component

private const val DEFAULT_TIMEZONE = "UTC"
private const val ACTIVE_STATUS_MARKER = "🟢"
private const val PAUSED_STATUS_MARKER = "⏸️"
private const val LIST_LINE_SEPARATOR = "\n"
private val VALID_HOUR_RANGE = 0..23
private val VALID_MINUTE_RANGE = 0..59

@Component
class NotificationAiHandler(
    private val notificationService: NotificationService,
    private val localizationService: LocalizationService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.ListNotifications,
        is BotCommand.CreateNotification,
        is BotCommand.DeleteNotification -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean =
        action is AiPendingAction.NotificationAction

    override fun prepareAction(
        command: BotCommand,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val lang = context.userInfo?.language
        val userId = context.userInfo?.telegramUserId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_user", lang))

        return when (command) {
            is BotCommand.ListNotifications -> AiPreparationResult.ImmediateResult(renderList(userId, lang))
            is BotCommand.CreateNotification -> prepareCreate(command, lang)
            is BotCommand.DeleteNotification -> prepareDelete(command.name, userId, lang)
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.NotificationAction) {
            return localizationService.t("ai.error.unknown_command", lang)
        }
        return when (action) {
            is AiPendingAction.NotificationAction.CreateDaily -> executeCreateDaily(action, context, lang)
            is AiPendingAction.NotificationAction.Delete -> executeDelete(action, lang)
        }
    }

    private fun renderList(userId: Long, lang: String?): String {
        val notifications = notificationService.getNotifications(userId)
        if (notifications.isEmpty()) return localizationService.t("ai.notification.list.empty", lang)

        val lines = notifications.joinToString(LIST_LINE_SEPARATOR) { notification ->
            val status = if (notification.isActive) ACTIVE_STATUS_MARKER else PAUSED_STATUS_MARKER
            val frequency = formatFrequencyShort(notification, localizationService, lang)
            localizationService.t(
                "ai.notification.list.item",
                lang,
                status,
                formatIconPrefix(notification.icon),
                notification.name,
                frequency
            )
        }
        return localizationService.t("ai.notification.list.title", lang) +
            LIST_LINE_SEPARATOR + LIST_LINE_SEPARATOR + lines
    }

    private fun prepareCreate(command: BotCommand.CreateNotification, lang: String?): AiPreparationResult {
        if (command.hour !in VALID_HOUR_RANGE || command.minute !in VALID_MINUTE_RANGE) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.notification.invalid_time", lang))
        }
        return AiPreparationResult.RequiresConfirmation(
            AiPendingAction.NotificationAction.CreateDaily(command.name, command.hour, command.minute)
        )
    }

    private fun prepareDelete(name: String, userId: Long, lang: String?): AiPreparationResult {
        val matches = findNotifications(userId, name)
        return when {
            matches.isEmpty() -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.notification.not_found", lang, name)
            )
            matches.size > 1 -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.notification.ambiguous", lang, name)
            )
            else -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.NotificationAction.Delete(matches.first())
            )
        }
    }

    private fun executeCreateDaily(
        action: AiPendingAction.NotificationAction.CreateDaily,
        context: MoneyManagerContext,
        lang: String?
    ): String {
        val userId = context.userInfo?.telegramUserId
            ?: return localizationService.t("ai.error.no_user", lang)
        val timezone = context.userInfo?.timezone

        notificationService.createNotification(
            telegramUserId = userId,
            name = action.name,
            icon = null,
            frequencyType = FrequencyType.DAILY,
            customInterval = null,
            hour = action.hour,
            minute = action.minute,
            dayOfWeek = null,
            dayOfMonth = null,
            monthOfYear = null,
            userTimezone = timezone ?: DEFAULT_TIMEZONE
        )

        val message = localizationService.t(
            "ai.notification.created",
            lang,
            action.name,
            formatTime(action.hour, action.minute)
        )
        if (timezone != null) return message
        return message + localizationService.t("ai.notification.created.timezone_hint", lang)
    }

    private fun executeDelete(action: AiPendingAction.NotificationAction.Delete, lang: String?): String {
        val notificationId = action.notification.id
            ?: return localizationService.t("ai.notification.delete.failed", lang, action.notification.name)
        return if (notificationService.deleteNotification(notificationId)) {
            localizationService.t("ai.notification.deleted", lang, action.notification.name)
        } else {
            localizationService.t("ai.notification.delete.failed", lang, action.notification.name)
        }
    }

    private fun findNotifications(userId: Long, name: String): List<NotificationEntity> =
        notificationService.getNotifications(userId).filter { matchesEntityName(it.name, name) }
}
