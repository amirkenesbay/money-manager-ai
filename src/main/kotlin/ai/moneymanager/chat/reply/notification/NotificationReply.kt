package ai.moneymanager.chat.reply.notification

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.chat.transition.notification.POPULAR_TIMEZONES
import ai.moneymanager.chat.transition.notification.EXTENDED_TIMEZONES
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private const val DEFAULT_NOTIFICATION_ICON = "🔔"
private const val ACTIVE_STATUS_ICON = "🟢"
private const val PAUSED_STATUS_ICON = "⏸️"

// ================================
// TIMEZONE REPLIES
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationTimezoneSelectReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT
        message {
            val lang = context.userInfo?.language
            val title = localizationService.t("notification.timezone.select.title", lang)
            val body = localizationService.t("notification.timezone.select.body", lang)
            text = """
                |$title
                |
                |$body
            """.trimMargin()

            keyboard {
                POPULAR_TIMEZONES.chunked(2).forEach { row ->
                    buttonRow {
                        row.forEach { tz ->
                            button {
                                text = tz
                                type = MoneyManagerButtonType.TIMEZONE_ITEM
                            }
                        }
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.timezone.button.other", lang)
                        type = MoneyManagerButtonType.TIMEZONE_OTHER
                    }
                }
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationTimezoneExtendedReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_TIMEZONE_EXTENDED
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.timezone.extended.title", lang)

            keyboard {
                EXTENDED_TIMEZONES.chunked(2).forEach { row ->
                    buttonRow {
                        row.forEach { tz ->
                            button {
                                text = tz
                                type = MoneyManagerButtonType.TIMEZONE_ITEM
                            }
                        }
                    }
                }
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}

// ================================
// NOTIFICATION LIST
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationListReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_LIST
        message {
            val lang = context.userInfo?.language
            val notifications = context.notifications
            val title = localizationService.t("notification.list.title", lang)

            text = if (notifications.isEmpty()) {
                """
                    |$title
                    |
                    |${localizationService.t("notification.list.empty", lang)}
                """.trimMargin()
            } else {
                val list = notifications.mapIndexed { index, n ->
                    val statusIcon = if (n.isActive) ACTIVE_STATUS_ICON else PAUSED_STATUS_ICON
                    val icon = n.icon ?: DEFAULT_NOTIFICATION_ICON
                    val freq = formatFrequencyShort(n, localizationService, lang)
                    "${index + 1}. $statusIcon $icon ${n.name}\n     $freq"
                }.joinToString("\n\n")

                """
                    |$title
                    |
                    |$list
                """.trimMargin()
            }

            keyboard {
                notifications.forEach { n ->
                    val statusIcon = if (n.isActive) ACTIVE_STATUS_ICON else PAUSED_STATUS_ICON
                    val icon = n.icon ?: DEFAULT_NOTIFICATION_ICON
                    buttonRow {
                        button {
                            text = "$statusIcon $icon ${n.name}"
                            type = MoneyManagerButtonType.NOTIFICATION_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.list.button.create", lang)
                        type = MoneyManagerButtonType.CREATE_NOTIFICATION
                    }
                }
                if (notifications.size > 1) {
                    buttonRow {
                        button {
                            text = localizationService.t("notification.list.button.delete_all", lang)
                            type = MoneyManagerButtonType.DELETE_ALL_NOTIFICATIONS
                        }
                    }
                }
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}

// ================================
// NOTIFICATION ACTIONS
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationActionsReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_ACTIONS
        message {
            val lang = context.userInfo?.language
            val n = context.currentNotification
            val icon = n?.icon ?: DEFAULT_NOTIFICATION_ICON
            val statusKey = if (n?.isActive == true) "notification.actions.status.active" else "notification.actions.status.paused"
            val statusText = localizationService.t(statusKey, lang)
            val freq = if (n != null) formatFrequencyFull(n, localizationService, lang) else ""
            val name = n?.name ?: localizationService.t("notification.actions.fallback_name", lang)

            text = """
                |$icon $name
                |
                |$statusText
                |$freq
            """.trimMargin()

            keyboard {
                val toggleKey = if (n?.isActive == true) "notification.actions.button.toggle.pause" else "notification.actions.button.toggle.activate"
                buttonRow {
                    button {
                        text = localizationService.t("notification.actions.button.edit", lang)
                        type = MoneyManagerButtonType.EDIT_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t(toggleKey, lang)
                        type = MoneyManagerButtonType.TOGGLE_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.actions.button.delete", lang)
                        type = MoneyManagerButtonType.DELETE_NOTIFICATION
                    }
                }
                backButton(
                    text = localizationService.t("common.back_to_list", lang),
                    type = MoneyManagerButtonType.BACK_TO_NOTIFICATIONS
                )
            }
        }
    }
}

// ================================
// CREATE FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateIconReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_ICON
        message {
            val lang = context.userInfo?.language
            val hasError = context.iconInputError
            val isEdit = context.notifEditMode
            val currentIcon = if (isEdit) context.currentNotification?.icon else null

            text = when {
                hasError -> localizationService.t("notification.create.icon.invalid", lang)
                isEdit && currentIcon != null -> localizationService.t("notification.create.icon.edit", lang, currentIcon)
                else -> localizationService.t("notification.create.icon.prompt", lang)
            }

            keyboard {
                if (!isEdit) {
                    buttonRow {
                        button {
                            text = localizationService.t("notification.create.icon.button.skip", lang)
                            type = MoneyManagerButtonType.NOTIFICATION_SKIP_ICON
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_NAME
        message {
            val lang = context.userInfo?.language
            val isCustomInput = context.customNameInputMode

            text = when {
                context.notifNameInputError -> localizationService.t("notification.create.name.error", lang)
                isCustomInput -> localizationService.t("notification.create.name.custom_prompt", lang)
                else -> localizationService.t("notification.create.name.quick_prompt", lang)
            }

            keyboard {
                if (!isCustomInput) {
                    QuickTemplates.NOTIFICATIONS.chunked(2).forEach { row ->
                        buttonRow {
                            row.forEach { template ->
                                button {
                                    text = "${template.icon} ${template.name}"
                                    type = template.buttonType
                                }
                            }
                        }
                    }
                    buttonRow {
                        button {
                            text = localizationService.t("notification.create.name.button.custom", lang)
                            type = MoneyManagerButtonType.ENTER_CUSTOM_NAME
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateFrequencyReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
        message {
            val lang = context.userInfo?.language
            text = localizationService.t(
                "notification.create.frequency.title",
                lang,
                context.notifNameInput ?: ""
            )

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.frequency.button.daily", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_DAILY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.frequency.button.weekly", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_WEEKLY
                    }
                    button {
                        text = localizationService.t("notification.create.frequency.button.biweekly", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_BIWEEKLY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.frequency.button.monthly", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_MONTHLY
                    }
                    button {
                        text = localizationService.t("notification.create.frequency.button.bimonthly", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_BIMONTHLY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.frequency.button.yearly", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_YEARLY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.frequency.button.custom", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_FREQ_CUSTOM
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomUnitReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_UNIT
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.create.custom_unit.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.custom_unit.button.days", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_DAYS
                    }
                    button {
                        text = localizationService.t("notification.create.custom_unit.button.weeks", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_WEEKS
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.create.custom_unit.button.months", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_MONTHS
                    }
                    button {
                        text = localizationService.t("notification.create.custom_unit.button.years", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_YEARS
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomNReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
        message {
            val lang = context.userInfo?.language
            val unitKey = when (context.notifFrequencyType) {
                FrequencyType.CUSTOM_DAYS -> "notification.create.custom_n.unit.days"
                FrequencyType.CUSTOM_WEEKS -> "notification.create.custom_n.unit.weeks"
                FrequencyType.CUSTOM_MONTHS -> "notification.create.custom_n.unit.months"
                FrequencyType.CUSTOM_YEARS -> "notification.create.custom_n.unit.years"
                else -> null
            }
            val unitLabel = unitKey?.let { localizationService.t(it, lang) } ?: ""

            text = if (context.notifCustomInputError) {
                localizationService.t("notification.create.custom_n.error", lang, unitLabel)
            } else {
                localizationService.t("notification.create.custom_n.prompt", lang, unitLabel)
            }

            keyboard {
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateDayOfWeekReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.create.day_of_week.title", lang)

            val locale = localeFor(lang)
            val shortNames = DayOfWeek.entries.map { it to dowShort(it, locale) }

            keyboard {
                shortNames.take(4).let { firstRow ->
                    buttonRow {
                        firstRow.forEach { (_, name) ->
                            button { text = name; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                        }
                    }
                }
                shortNames.drop(4).let { secondRow ->
                    buttonRow {
                        secondRow.forEach { (_, name) ->
                            button { text = name; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateDayOfMonthReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.create.day_of_month.title", lang)

            keyboard {
                (1..31).chunked(7).forEach { row ->
                    buttonRow {
                        row.forEach { day ->
                            button { text = day.toString(); type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_MONTH }
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateMonthReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_MONTH
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.create.month.title", lang)

            val locale = localeFor(lang)
            keyboard {
                Month.entries.chunked(3).forEach { row ->
                    buttonRow {
                        row.forEach { month ->
                            button { text = monthFull(month, locale); type = MoneyManagerButtonType.NOTIFICATION_MONTH_ITEM }
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectHourReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
        message {
            val lang = context.userInfo?.language
            text = localizationService.t("notification.create.hour.title", lang)

            keyboard {
                (0..23).chunked(6).forEach { row ->
                    buttonRow {
                        row.forEach { hour ->
                            button {
                                text = "%02d".format(hour)
                                type = MoneyManagerButtonType.NOTIFICATION_HOUR
                            }
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectMinuteReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_MINUTE
        message {
            val lang = context.userInfo?.language
            text = localizationService.t(
                "notification.create.minute.title",
                lang,
                "%02d".format(context.notifHour)
            )

            keyboard {
                (0..55 step 5).chunked(6).forEach { row ->
                    buttonRow {
                        row.forEach { minute ->
                            button {
                                text = "%02d".format(minute)
                                type = MoneyManagerButtonType.NOTIFICATION_MINUTE
                            }
                        }
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
        message {
            val lang = context.userInfo?.language
            val summary = buildNotificationSummary(context, localizationService, lang)
            val isEdit = context.notifEditMode

            text = if (isEdit) {
                localizationService.t("notification.create.confirm.title.edit", lang, summary)
            } else {
                localizationService.t("notification.create.confirm.title.create", lang, summary)
            }

            keyboard {
                val confirmKey = if (isEdit) "notification.create.confirm.button.save" else "notification.create.confirm.button.create"
                buttonRow {
                    button {
                        text = localizationService.t(confirmKey, lang)
                        type = MoneyManagerButtonType.NOTIFICATION_CONFIRM_CREATE
                    }
                }
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

// ================================
// EDIT FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditMenuReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_MENU
        message {
            val lang = context.userInfo?.language
            val n = context.currentNotification
            text = localizationService.t("notification.edit.menu.title", lang, n?.name ?: "")

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("notification.edit.menu.button.icon", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_EDIT_ICON_BTN
                    }
                    button {
                        text = localizationService.t("notification.edit.menu.button.name", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_EDIT_NAME_BTN
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("notification.edit.menu.button.frequency", lang)
                        type = MoneyManagerButtonType.NOTIFICATION_EDIT_FREQUENCY_BTN
                    }
                }
                backButton(
                    text = localizationService.t("common.back", lang),
                    type = MoneyManagerButtonType.BACK_TO_NOTIFICATIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_NAME
        message {
            val lang = context.userInfo?.language
            text = localizationService.t(
                "notification.edit.name.prompt",
                lang,
                context.currentNotification?.name ?: ""
            )

            keyboard {
                cancelButton(text = localizationService.t("common.cancel", lang))
            }
        }
    }
}

// ================================
// DELETE FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM
        message {
            val lang = context.userInfo?.language
            val n = context.currentNotification
            val freqLine = if (n != null) formatFrequencyFull(n, localizationService, lang) else ""
            text = localizationService.t(
                "notification.delete.confirm.title",
                lang,
                n?.name ?: "",
                freqLine
            )

            keyboard {
                confirmAndCancelButtons(
                    confirmText = localizationService.t("notification.delete.confirm.button", lang),
                    cancelText = localizationService.t("common.cancel", lang)
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteAllConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM
        message {
            val lang = context.userInfo?.language
            val count = context.notifications.size
            text = localizationService.t("notification.delete_all.confirm.title", lang, count)

            keyboard {
                confirmAndCancelButtons(
                    confirmText = localizationService.t("notification.delete_all.confirm.button", lang),
                    cancelText = localizationService.t("common.cancel", lang)
                )
            }
        }
    }
}

// ================================
// FORMAT HELPERS
// ================================

fun parseDayOfWeek(text: String, language: String?): DayOfWeek? {
    val candidates = listOfNotNull(language, LocalizationService.FALLBACK_LANGUAGE) + LocalizationService.SUPPORTED_LANGUAGES
    return candidates.distinct().firstNotNullOfOrNull { lang ->
        val locale = localeFor(lang)
        DayOfWeek.entries.find { dowShort(it, locale).equals(text, ignoreCase = true) }
    }
}

fun parseMonth(text: String, language: String?): Int? {
    val candidates = listOfNotNull(language, LocalizationService.FALLBACK_LANGUAGE) + LocalizationService.SUPPORTED_LANGUAGES
    return candidates.distinct().firstNotNullOfOrNull { lang ->
        val locale = localeFor(lang)
        Month.entries.find { monthFull(it, locale).equals(text, ignoreCase = true) }?.value
    }
}

fun formatFrequencyShort(
    n: NotificationEntity,
    localizationService: LocalizationService,
    language: String?
): String {
    val timeStr = "%02d:%02d".format(n.hour, n.minute)
    val dowName = dayOfWeekName(n.dayOfWeek, language)
    val monthN = monthName(n.monthOfYear, language)
    return when (n.frequencyType) {
        FrequencyType.DAILY -> localizationService.t("notification.frequency.short.daily", language, timeStr)
        FrequencyType.WEEKLY -> localizationService.t("notification.frequency.short.weekly", language, dowName, timeStr)
        FrequencyType.BIWEEKLY -> localizationService.t("notification.frequency.short.biweekly", language, dowName, timeStr)
        FrequencyType.MONTHLY -> localizationService.t("notification.frequency.short.monthly", language, n.dayOfMonth ?: 1, timeStr)
        FrequencyType.BIMONTHLY -> localizationService.t("notification.frequency.short.bimonthly", language, n.dayOfMonth ?: 1, timeStr)
        FrequencyType.YEARLY -> localizationService.t("notification.frequency.short.yearly", language, n.dayOfMonth ?: 1, monthN, timeStr)
        FrequencyType.CUSTOM_DAYS -> localizationService.t("notification.frequency.short.custom_days", language, n.customInterval ?: 1, timeStr)
        FrequencyType.CUSTOM_WEEKS -> localizationService.t("notification.frequency.short.custom_weeks", language, n.customInterval ?: 1, dowName, timeStr)
        FrequencyType.CUSTOM_MONTHS -> localizationService.t("notification.frequency.short.custom_months", language, n.customInterval ?: 1, n.dayOfMonth ?: 1, timeStr)
        FrequencyType.CUSTOM_YEARS -> localizationService.t("notification.frequency.short.custom_years", language, n.customInterval ?: 1, n.dayOfMonth ?: 1, monthN, timeStr)
    }
}

fun formatFrequencyFull(
    n: NotificationEntity,
    localizationService: LocalizationService,
    language: String?
): String = localizationService.t("notification.frequency.full", language, formatFrequencyShort(n, localizationService, language))

fun buildNotificationSummary(
    context: MoneyManagerContext,
    localizationService: LocalizationService,
    language: String?
): String {
    val timeStr = "%02d:%02d".format(context.notifHour ?: 0, context.notifMinute ?: 0)
    val freqStr = buildFrequencyString(context, localizationService, language)
    val name = context.notifNameInput ?: context.currentNotification?.name ?: ""
    val icon = context.notifIconInput ?: context.currentNotification?.icon

    val nameLine = localizationService.t("notification.summary.name", language, name)
    val iconLine = if (icon != null) "\n${localizationService.t("notification.summary.icon", language, icon)}" else ""
    val freqLine = localizationService.t("notification.summary.frequency", language, freqStr)
    val timeLine = localizationService.t("notification.summary.time", language, timeStr)

    return """
        |$nameLine$iconLine
        |$freqLine
        |$timeLine
    """.trimMargin()
}

private fun buildFrequencyString(
    ctx: MoneyManagerContext,
    localizationService: LocalizationService,
    language: String?
): String {
    val dowName = dayOfWeekName(ctx.notifDayOfWeek, language)
    val monthN = monthName(ctx.notifMonthOfYear, language)
    return when (ctx.notifFrequencyType) {
        FrequencyType.DAILY -> localizationService.t("notification.frequency.summary.daily", language)
        FrequencyType.WEEKLY -> localizationService.t("notification.frequency.summary.weekly", language, dowName)
        FrequencyType.BIWEEKLY -> localizationService.t("notification.frequency.summary.biweekly", language, dowName)
        FrequencyType.MONTHLY -> localizationService.t("notification.frequency.summary.monthly", language, ctx.notifDayOfMonth ?: 1)
        FrequencyType.BIMONTHLY -> localizationService.t("notification.frequency.summary.bimonthly", language, ctx.notifDayOfMonth ?: 1)
        FrequencyType.YEARLY -> localizationService.t("notification.frequency.summary.yearly", language, ctx.notifDayOfMonth ?: 1, monthN)
        FrequencyType.CUSTOM_DAYS -> localizationService.t("notification.frequency.summary.custom_days", language, ctx.notifCustomInterval ?: 1)
        FrequencyType.CUSTOM_WEEKS -> localizationService.t("notification.frequency.summary.custom_weeks", language, ctx.notifCustomInterval ?: 1, dowName)
        FrequencyType.CUSTOM_MONTHS -> localizationService.t("notification.frequency.summary.custom_months", language, ctx.notifCustomInterval ?: 1, ctx.notifDayOfMonth ?: 1)
        FrequencyType.CUSTOM_YEARS -> localizationService.t("notification.frequency.summary.custom_years", language, ctx.notifCustomInterval ?: 1, ctx.notifDayOfMonth ?: 1, monthN)
        null -> ""
    }
}

private fun dayOfWeekName(dow: DayOfWeek?, language: String?): String {
    if (dow == null) return ""
    return dow.getDisplayName(TextStyle.FULL, localeFor(language))
}

private fun monthName(month: Int?, language: String?): String {
    if (month == null) return ""
    return Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, localeFor(language))
}

private fun dowShort(dow: DayOfWeek, locale: Locale): String =
    dow.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercaseChar() }

private fun monthFull(month: Month, locale: Locale): String =
    month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercaseChar() }

private fun localeFor(language: String?): Locale =
    Locale.of(language ?: LocalizationService.FALLBACK_LANGUAGE)
