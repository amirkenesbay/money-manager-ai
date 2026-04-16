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
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

// ================================
// TIMEZONE REPLIES
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationTimezoneSelectReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT
        message {
            text = """
                |🌍 Выберите ваш часовой пояс
                |
                |Это нужно для корректной отправки уведомлений.
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
                        text = "🌐 Другой"
                        type = MoneyManagerButtonType.TIMEZONE_OTHER
                    }
                }
                backButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationTimezoneExtendedReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_TIMEZONE_EXTENDED
        message {
            text = """
                |🌍 Выберите часовой пояс
            """.trimMargin()

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
                backButton()
            }
        }
    }
}

// ================================
// NOTIFICATION LIST
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationListReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_LIST
        message {
            val notifications = context.notifications

            text = if (notifications.isEmpty()) {
                """
                    |🔔 Уведомления
                    |
                    |У вас пока нет уведомлений.
                    |Создайте первое уведомление, чтобы не забывать вносить расходы и доходы.
                """.trimMargin()
            } else {
                val list = notifications.mapIndexed { index, n ->
                    val statusIcon = if (n.isActive) "🟢" else "⏸️"
                    val icon = n.icon ?: "🔔"
                    val freq = formatFrequencyShort(n)
                    "${index + 1}. $statusIcon $icon ${n.name}\n     $freq"
                }.joinToString("\n\n")

                """
                    |🔔 Уведомления
                    |
                    |$list
                """.trimMargin()
            }

            keyboard {
                notifications.forEach { n ->
                    val statusIcon = if (n.isActive) "🟢" else "⏸️"
                    val icon = n.icon ?: "🔔"
                    buttonRow {
                        button {
                            text = "$statusIcon $icon ${n.name}"
                            type = MoneyManagerButtonType.NOTIFICATION_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "➕ Создать уведомление"
                        type = MoneyManagerButtonType.CREATE_NOTIFICATION
                    }
                }
                if (notifications.size > 1) {
                    buttonRow {
                        button {
                            text = "🗑 Удалить все"
                            type = MoneyManagerButtonType.DELETE_ALL_NOTIFICATIONS
                        }
                    }
                }
                backButton()
            }
        }
    }
}

// ================================
// NOTIFICATION ACTIONS
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationActionsReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_ACTIONS
        message {
            val n = context.currentNotification
            val icon = n?.icon ?: "🔔"
            val statusText = if (n?.isActive == true) "🟢 Активно" else "⏸️ На паузе"
            val freq = if (n != null) formatFrequencyFull(n) else ""

            text = """
                |$icon ${n?.name ?: "Уведомление"}
                |
                |$statusText
                |$freq
            """.trimMargin()

            keyboard {
                val toggleText = if (n?.isActive == true) "⏸️ Поставить на паузу" else "▶️ Включить"
                buttonRow {
                    button {
                        text = "✏️ Редактировать"
                        type = MoneyManagerButtonType.EDIT_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = toggleText
                        type = MoneyManagerButtonType.TOGGLE_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = "🗑 Удалить"
                        type = MoneyManagerButtonType.DELETE_NOTIFICATION
                    }
                }
                backButton(type = MoneyManagerButtonType.BACK_TO_NOTIFICATIONS)
            }
        }
    }
}

// ================================
// CREATE FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateIconReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_ICON
        message {
            val hasError = context.iconInputError
            val isEdit = context.notifEditMode
            val currentIcon = if (isEdit) context.currentNotification?.icon else null

            text = if (hasError) {
                """
                    |⚠️ Пожалуйста, отправьте эмодзи, а не текст.
                    |
                    |🎨 Отправьте эмодзи для уведомления:
                """.trimMargin()
            } else if (isEdit && currentIcon != null) {
                """
                    |🎨 Текущая иконка: $currentIcon
                    |
                    |Отправьте новый эмодзи:
                """.trimMargin()
            } else {
                """
                    |🎨 Отправьте эмодзи для уведомления
                    |
                    |Например: 📝 💰 🏠 🎯
                """.trimMargin()
            }

            keyboard {
                if (!isEdit) {
                    buttonRow {
                        button {
                            text = "⏭ Пропустить"
                            type = MoneyManagerButtonType.NOTIFICATION_SKIP_ICON
                        }
                    }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateNameReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_NAME
        message {
            val isCustomInput = context.customNameInputMode

            text = if (context.notifNameInputError) {
                """
                    |❌ Название не может быть пустым или длиннее 100 символов.
                    |
                    |✏️ Введите название уведомления:
                """.trimMargin()
            } else if (isCustomInput) {
                "✍️ Введите название уведомления:"
            } else {
                """
                    |🔔 Выберите готовый вариант или задайте своё название:
                """.trimMargin()
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
                            text = "✏️ Своё название"
                            type = MoneyManagerButtonType.ENTER_CUSTOM_NAME
                        }
                    }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateFrequencyReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
        message {
            text = """
                |📋 ${context.notifNameInput ?: ""}
                |
                |⏰ Выберите периодичность:
            """.trimMargin()

            keyboard {
                buttonRow {
                    button { text = "Каждый день"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_DAILY }
                }
                buttonRow {
                    button { text = "Каждую неделю"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_WEEKLY }
                    button { text = "Каждые 2 недели"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_BIWEEKLY }
                }
                buttonRow {
                    button { text = "Каждый месяц"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_MONTHLY }
                    button { text = "Каждые 2 месяца"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_BIMONTHLY }
                }
                buttonRow {
                    button { text = "Каждый год"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_YEARLY }
                }
                buttonRow {
                    button { text = "⚙️ Своя периодичность"; type = MoneyManagerButtonType.NOTIFICATION_FREQ_CUSTOM }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomUnitReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_UNIT
        message {
            text = "⚙️ Выберите единицу периодичности:"

            keyboard {
                buttonRow {
                    button { text = "Дни"; type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_DAYS }
                    button { text = "Недели"; type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_WEEKS }
                }
                buttonRow {
                    button { text = "Месяцы"; type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_MONTHS }
                    button { text = "Годы"; type = MoneyManagerButtonType.NOTIFICATION_CUSTOM_YEARS }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomNReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
        message {
            val unitLabel = when (context.notifFrequencyType) {
                FrequencyType.CUSTOM_DAYS -> "дней (1–365)"
                FrequencyType.CUSTOM_WEEKS -> "недель (1–52)"
                FrequencyType.CUSTOM_MONTHS -> "месяцев (1–24)"
                FrequencyType.CUSTOM_YEARS -> "лет (1–10)"
                else -> ""
            }

            text = if (context.notifCustomInputError) {
                "❌ Некорректное значение.\n\n🔢 Введите количество $unitLabel:"
            } else {
                "🔢 Введите количество $unitLabel:"
            }

            keyboard {
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateDayOfWeekReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK
        message {
            text = "📅 Выберите день недели:"

            keyboard {
                buttonRow {
                    button { text = "Пн"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                    button { text = "Вт"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                    button { text = "Ср"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                    button { text = "Чт"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                }
                buttonRow {
                    button { text = "Пт"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                    button { text = "Сб"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                    button { text = "Вс"; type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateDayOfMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH
        message {
            text = "📅 Выберите день месяца:"

            keyboard {
                (1..31).chunked(7).forEach { row ->
                    buttonRow {
                        row.forEach { day ->
                            button { text = day.toString(); type = MoneyManagerButtonType.NOTIFICATION_DAY_OF_MONTH }
                        }
                    }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_MONTH
        message {
            text = "📅 Выберите месяц:"

            keyboard {
                val ruLocale = Locale.of("ru")
                Month.entries.chunked(3).forEach { row ->
                    buttonRow {
                        row.forEach { month ->
                            val name = month.getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
                                .replaceFirstChar { it.uppercaseChar() }
                            button { text = name; type = MoneyManagerButtonType.NOTIFICATION_MONTH_ITEM }
                        }
                    }
                }
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectHourReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
        message {
            text = "🕐 Выберите час:"

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
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectMinuteReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_MINUTE
        message {
            text = "🕐 Час: ${"%02d".format(context.notifHour)}\n\n🕑 Выберите минуты:"

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
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateConfirmReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
        message {
            val summary = buildNotificationSummary(context)
            val isEdit = context.notifEditMode

            text = if (isEdit) {
                """
                    |✏️ Подтвердите изменения
                    |
                    |$summary
                """.trimMargin()
            } else {
                """
                    |✅ Подтвердите создание уведомления
                    |
                    |$summary
                """.trimMargin()
            }

            keyboard {
                val confirmText = if (isEdit) "✅ Сохранить" else "✅ Создать"
                buttonRow {
                    button {
                        text = confirmText
                        type = MoneyManagerButtonType.NOTIFICATION_CONFIRM_CREATE
                    }
                }
                cancelButton()
            }
        }
    }
}

// ================================
// EDIT FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditMenuReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_MENU
        message {
            val n = context.currentNotification
            text = """
                |✏️ Редактирование: ${n?.name ?: ""}
                |
                |Что хотите изменить?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button { text = "🎨 Иконка"; type = MoneyManagerButtonType.NOTIFICATION_EDIT_ICON_BTN }
                    button { text = "📝 Название"; type = MoneyManagerButtonType.NOTIFICATION_EDIT_NAME_BTN }
                }
                buttonRow {
                    button { text = "⏰ Периодичность и время"; type = MoneyManagerButtonType.NOTIFICATION_EDIT_FREQUENCY_BTN }
                }
                backButton(type = MoneyManagerButtonType.BACK_TO_NOTIFICATIONS)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditNameReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_NAME
        message {
            text = """
                |📝 Текущее название: ${context.currentNotification?.name ?: ""}
                |
                |Введите новое название:
            """.trimMargin()

            keyboard {
                cancelButton()
            }
        }
    }
}

// ================================
// DELETE FLOW
// ================================

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteConfirmReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM
        message {
            val n = context.currentNotification
            text = """
                |⚠️ Удалить уведомление?
                |
                |🔔 ${n?.name ?: ""}
                |${if (n != null) formatFrequencyFull(n) else ""}
                |
                |Это действие необратимо.
            """.trimMargin()

            keyboard {
                confirmAndCancelButtons("✅ Да, удалить")
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteAllConfirmReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM
        message {
            val count = context.notifications.size
            text = """
                |⚠️ Удалить все уведомления?
                |
                |Всего уведомлений: $count
                |
                |Это действие необратимо.
            """.trimMargin()

            keyboard {
                confirmAndCancelButtons("✅ Да, удалить все")
            }
        }
    }
}

// ================================
// FORMAT HELPERS
// ================================

private val DAY_OF_WEEK_SHORT = mapOf(
    "Пн" to DayOfWeek.MONDAY,
    "Вт" to DayOfWeek.TUESDAY,
    "Ср" to DayOfWeek.WEDNESDAY,
    "Чт" to DayOfWeek.THURSDAY,
    "Пт" to DayOfWeek.FRIDAY,
    "Сб" to DayOfWeek.SATURDAY,
    "Вс" to DayOfWeek.SUNDAY
)

fun parseDayOfWeek(text: String): DayOfWeek? = DAY_OF_WEEK_SHORT[text]

fun parseMonth(text: String): Int? {
    val ruLocale = Locale.of("ru")
    return Month.entries.find { month ->
        month.getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
            .replaceFirstChar { it.uppercaseChar() } == text
    }?.value
}

fun formatFrequencyShort(n: NotificationEntity): String {
    val timeStr = "%02d:%02d".format(n.hour, n.minute)
    return when (n.frequencyType) {
        FrequencyType.DAILY -> "Каждый день в $timeStr"
        FrequencyType.WEEKLY -> "Каждую неделю, ${dayOfWeekName(n.dayOfWeek)} в $timeStr"
        FrequencyType.BIWEEKLY -> "Каждые 2 недели, ${dayOfWeekName(n.dayOfWeek)} в $timeStr"
        FrequencyType.MONTHLY -> "Каждый месяц, ${n.dayOfMonth}-го в $timeStr"
        FrequencyType.BIMONTHLY -> "Каждые 2 месяца, ${n.dayOfMonth}-го в $timeStr"
        FrequencyType.YEARLY -> "Каждый год, ${n.dayOfMonth} ${monthName(n.monthOfYear)} в $timeStr"
        FrequencyType.CUSTOM_DAYS -> "Каждые ${n.customInterval} дн. в $timeStr"
        FrequencyType.CUSTOM_WEEKS -> "Каждые ${n.customInterval} нед., ${dayOfWeekName(n.dayOfWeek)} в $timeStr"
        FrequencyType.CUSTOM_MONTHS -> "Каждые ${n.customInterval} мес., ${n.dayOfMonth}-го в $timeStr"
        FrequencyType.CUSTOM_YEARS -> "Каждые ${n.customInterval} г., ${n.dayOfMonth} ${monthName(n.monthOfYear)} в $timeStr"
    }
}

fun formatFrequencyFull(n: NotificationEntity): String {
    val timeStr = "%02d:%02d".format(n.hour, n.minute)
    return """
        |⏰ ${formatFrequencyShort(n)}
    """.trimMargin()
}

fun buildNotificationSummary(context: MoneyManagerContext): String {
    val timeStr = "%02d:%02d".format(context.notifHour ?: 0, context.notifMinute ?: 0)
    val freqStr = buildFrequencyString(context)
    val icon = context.notifIconInput ?: context.currentNotification?.icon
    val iconLine = if (icon != null) "\n|🎨 Иконка: $icon" else ""

    return """
        |📋 Название: ${context.notifNameInput ?: context.currentNotification?.name ?: ""}$iconLine
        |⏰ Периодичность: $freqStr
        |🕐 Время: $timeStr
    """.trimMargin()
}

private fun buildFrequencyString(ctx: MoneyManagerContext): String {
    return when (ctx.notifFrequencyType) {
        FrequencyType.DAILY -> "Каждый день"
        FrequencyType.WEEKLY -> "Каждую неделю, ${dayOfWeekName(ctx.notifDayOfWeek)}"
        FrequencyType.BIWEEKLY -> "Каждые 2 недели, ${dayOfWeekName(ctx.notifDayOfWeek)}"
        FrequencyType.MONTHLY -> "Каждый месяц, ${ctx.notifDayOfMonth}-го"
        FrequencyType.BIMONTHLY -> "Каждые 2 месяца, ${ctx.notifDayOfMonth}-го"
        FrequencyType.YEARLY -> "Каждый год, ${ctx.notifDayOfMonth} ${monthName(ctx.notifMonthOfYear)}"
        FrequencyType.CUSTOM_DAYS -> "Каждые ${ctx.notifCustomInterval} дн."
        FrequencyType.CUSTOM_WEEKS -> "Каждые ${ctx.notifCustomInterval} нед., ${dayOfWeekName(ctx.notifDayOfWeek)}"
        FrequencyType.CUSTOM_MONTHS -> "Каждые ${ctx.notifCustomInterval} мес., ${ctx.notifDayOfMonth}-го"
        FrequencyType.CUSTOM_YEARS -> "Каждые ${ctx.notifCustomInterval} г., ${ctx.notifDayOfMonth} ${monthName(ctx.notifMonthOfYear)}"
        null -> ""
    }
}

private fun dayOfWeekName(dow: DayOfWeek?): String {
    if (dow == null) return ""
    val ruLocale = Locale.of("ru")
    return dow.getDisplayName(TextStyle.FULL, ruLocale)
}

private fun monthName(month: Int?): String {
    if (month == null) return ""
    val ruLocale = Locale.of("ru")
    return Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
}
