package ai.moneymanager.chat.reply.notification

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.NotificationPeriodType
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationListReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_LIST

        message {
            val notifications = context.notifications

            text = if (notifications.isEmpty()) {
                """
                    |🔔 Уведомления
                    |
                    |У вас пока нет уведомлений.
                    |Создайте уведомление, чтобы получать напоминания о внесении расходов и доходов.
                """.trimMargin()
            } else {
                val list = notifications.mapIndexed { index, notification ->
                    val periodicity = notificationService.formatPeriodicity(notification)
                    "${index + 1}. ${notification.title}\n   📅 $periodicity"
                }.joinToString("\n\n")

                """
                    |🔔 Уведомления
                    |
                    |$list
                """.trimMargin()
            }

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать уведомление"
                        type = MoneyManagerButtonType.CREATE_NOTIFICATION
                    }
                }
                if (notifications.isNotEmpty()) {
                    buttonRow {
                        button {
                            text = "✏️ Редактировать"
                            type = MoneyManagerButtonType.EDIT_NOTIFICATION
                        }
                    }
                    buttonRow {
                        button {
                            text = "🗑 Удалить"
                            type = MoneyManagerButtonType.DELETE_NOTIFICATION
                        }
                    }
                    buttonRow {
                        button {
                            text = "🗑 Удалить все"
                            type = MoneyManagerButtonType.DELETE_ALL_NOTIFICATIONS
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад в меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateEnterNameReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_ENTER_NAME

        message {
            text = """
                |➕ Создание уведомления
                |
                |Введите название уведомления (до 50 символов):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectPeriodReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_PERIOD

        message {
            val title = context.notificationTitleInput ?: "Напоминание"

            text = """
                |➕ Создание уведомления
                |
                |Название: $title
                |
                |Выберите периодичность:
            """.trimMargin()

            keyboard {
                periodButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomEnterNReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_ENTER_N

        message {
            text = """
                |➕ Пользовательская периодичность
                |
                |Введите число N (каждые N ...):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateCustomSelectUnitReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_SELECT_UNIT

        message {
            val n = context.notificationCustomNInput ?: 1

            text = """
                |➕ Пользовательская периодичность
                |
                |Каждые $n ...
                |
                |Выберите единицу измерения:
            """.trimMargin()

            keyboard {
                customUnitButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectDayOfWeekReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_DAY_OF_WEEK

        message {
            text = """
                |📅 Выбор дня недели
                |
                |Выберите день недели для уведомления:
            """.trimMargin()

            keyboard {
                dayOfWeekButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateEnterDayOfMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_ENTER_DAY_OF_MONTH

        message {
            text = """
                |📅 Выбор дня месяца
                |
                |Введите день месяца (1-28):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateSelectMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_SELECT_MONTH

        message {
            text = """
                |📅 Выбор месяца
                |
                |Выберите месяц:
            """.trimMargin()

            keyboard {
                monthButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateEnterTimeReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_ENTER_TIME

        message {
            text = """
                |⏰ Выбор времени
                |
                |Введите время в формате ЧЧ:ММ (например, 20:00):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateConfirmReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM

        message {
            val title = context.notificationTitleInput ?: "Напоминание"
            val periodType = context.notificationPeriodTypeInput ?: NotificationPeriodType.DAILY
            val hour = context.notificationHourInput ?: 12
            val minute = context.notificationMinuteInput ?: 0

            val periodicity = notificationService.formatPeriodicity(
                ai.moneymanager.domain.model.Notification(
                    userId = 0,
                    title = title,
                    periodType = periodType,
                    customN = context.notificationCustomNInput,
                    customUnit = context.notificationCustomUnitInput,
                    hour = hour,
                    minute = minute,
                    dayOfWeek = context.notificationDayOfWeekInput,
                    dayOfMonth = context.notificationDayOfMonthInput,
                    month = context.notificationMonthInput,
                    nextTriggerTime = java.time.LocalDateTime.now()
                )
            )

            text = """
                |✅ Подтверждение создания
                |
                |Название: $title
                |Периодичность: $periodicity
                |
                |Создать уведомление?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Создать"
                        type = MoneyManagerButtonType.CONFIRM_CREATE_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

// ----- Edit replies -----

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditSelectReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_SELECT

        message {
            val notifications = context.notifications

            text = """
                |✏️ Редактирование уведомления
                |
                |Выберите уведомление для редактирования:
            """.trimMargin()

            keyboard {
                notifications.forEachIndexed { index, notification ->
                    buttonRow {
                        button {
                            val periodicity = notificationService.formatPeriodicity(notification)
                            text = "${index + 1}. ${notification.title}"
                            type = MoneyManagerButtonType.NOTIFICATION_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditMenuReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_MENU

        message {
            val notification = context.currentNotification
            val title = notification?.title ?: "Напоминание"
            val periodicity = if (notification != null) notificationService.formatPeriodicity(notification) else ""

            text = """
                |✏️ Редактирование уведомления
                |
                |Название: $title
                |Периодичность: $periodicity
                |
                |Что хотите изменить?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✏️ Изменить название"
                        type = MoneyManagerButtonType.EDIT_NOTIFICATION_NAME
                    }
                }
                buttonRow {
                    button {
                        text = "📅 Изменить периодичность"
                        type = MoneyManagerButtonType.EDIT_NOTIFICATION_PERIOD
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditEnterNameReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_ENTER_NAME

        message {
            val currentName = context.currentNotification?.title ?: ""

            text = """
                |✏️ Изменение названия
                |
                |Текущее название: $currentName
                |
                |Введите новое название (до 50 символов):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditSelectPeriodReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_SELECT_PERIOD

        message {
            text = """
                |📅 Изменение периодичности
                |
                |Выберите новую периодичность:
            """.trimMargin()

            keyboard {
                periodButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditCustomEnterNReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_CUSTOM_ENTER_N

        message {
            text = """
                |📅 Пользовательская периодичность
                |
                |Введите число N (каждые N ...):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditCustomSelectUnitReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_CUSTOM_SELECT_UNIT

        message {
            val n = context.notificationCustomNInput ?: 1

            text = """
                |📅 Пользовательская периодичность
                |
                |Каждые $n ...
                |
                |Выберите единицу измерения:
            """.trimMargin()

            keyboard {
                customUnitButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditSelectDayOfWeekReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_SELECT_DAY_OF_WEEK

        message {
            text = """
                |📅 Выбор дня недели
                |
                |Выберите день недели для уведомления:
            """.trimMargin()

            keyboard {
                dayOfWeekButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditEnterDayOfMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_ENTER_DAY_OF_MONTH

        message {
            text = """
                |📅 Выбор дня месяца
                |
                |Введите день месяца (1-28):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditSelectMonthReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_SELECT_MONTH

        message {
            text = """
                |📅 Выбор месяца
                |
                |Выберите месяц:
            """.trimMargin()

            keyboard {
                monthButtons()
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditEnterTimeReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_ENTER_TIME

        message {
            text = """
                |⏰ Выбор времени
                |
                |Введите время в формате ЧЧ:ММ (например, 20:00):
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditConfirmReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_EDIT_CONFIRM

        message {
            val notification = context.currentNotification
            val title = notification?.title ?: "Напоминание"
            val periodType = context.notificationPeriodTypeInput ?: NotificationPeriodType.DAILY
            val hour = context.notificationHourInput ?: 12
            val minute = context.notificationMinuteInput ?: 0

            val periodicity = notificationService.formatPeriodicity(
                ai.moneymanager.domain.model.Notification(
                    userId = 0,
                    title = title,
                    periodType = periodType,
                    customN = context.notificationCustomNInput,
                    customUnit = context.notificationCustomUnitInput,
                    hour = hour,
                    minute = minute,
                    dayOfWeek = context.notificationDayOfWeekInput,
                    dayOfMonth = context.notificationDayOfMonthInput,
                    month = context.notificationMonthInput,
                    nextTriggerTime = java.time.LocalDateTime.now()
                )
            )

            text = """
                |✅ Подтверждение изменения
                |
                |Название: $title
                |Новая периодичность: $periodicity
                |
                |Сохранить изменения?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Сохранить"
                        type = MoneyManagerButtonType.CONFIRM_EDIT_NOTIFICATION
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

// ----- Delete replies -----

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteSelectReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_SELECT

        message {
            val notifications = context.notifications

            text = """
                |🗑 Удаление уведомления
                |
                |Выберите уведомление для удаления:
            """.trimMargin()

            keyboard {
                notifications.forEachIndexed { index, notification ->
                    buttonRow {
                        button {
                            text = "${index + 1}. ${notification.title}"
                            type = MoneyManagerButtonType.NOTIFICATION_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteConfirmReply(
    notificationService: NotificationService
) {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM

        message {
            val notification = context.currentNotification
            val title = notification?.title ?: "Напоминание"
            val periodicity = if (notification != null) notificationService.formatPeriodicity(notification) else ""

            text = """
                |🗑 Удаление уведомления
                |
                |Вы уверены, что хотите удалить уведомление?
                |
                |Название: $title
                |Периодичность: $periodicity
                |
                |⚠️ Это действие необратимо!
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Да, удалить"
                        type = MoneyManagerButtonType.CONFIRM_DELETE
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteAllConfirmReply() {
    reply {
        state = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM

        message {
            val count = context.notifications.size

            text = if (count == 0) {
                """
                    |❌ Нет уведомлений для удаления
                    |
                    |У вас нет уведомлений.
                """.trimMargin()
            } else {
                """
                    |⚠️ Удаление всех уведомлений
                    |
                    |Вы уверены, что хотите удалить ВСЕ уведомления?
                    |
                    |Всего уведомлений: $count
                    |
                    |⚠️ Это действие необратимо!
                """.trimMargin()
            }

            keyboard {
                if (count > 0) {
                    buttonRow {
                        button {
                            text = "✅ Да, удалить все"
                            type = MoneyManagerButtonType.CONFIRM_DELETE
                        }
                    }
                }
                buttonRow {
                    button {
                        text = if (count == 0) "⬅️ Назад" else "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

// ----- Helper keyboard builders -----

private fun kz.rmr.chatmachinist.api.reply.KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.periodButtons() {
    buttonRow {
        button {
            text = "📅 Каждый день"
            type = MoneyManagerButtonType.PERIOD_DAILY
        }
    }
    buttonRow {
        button {
            text = "📅 Каждую неделю"
            type = MoneyManagerButtonType.PERIOD_WEEKLY
        }
    }
    buttonRow {
        button {
            text = "📅 Каждые 2 недели"
            type = MoneyManagerButtonType.PERIOD_BIWEEKLY
        }
    }
    buttonRow {
        button {
            text = "📅 Каждый месяц"
            type = MoneyManagerButtonType.PERIOD_MONTHLY
        }
    }
    buttonRow {
        button {
            text = "📅 Каждые 2 месяца"
            type = MoneyManagerButtonType.PERIOD_BIMONTHLY
        }
    }
    buttonRow {
        button {
            text = "📅 Каждый год"
            type = MoneyManagerButtonType.PERIOD_YEARLY
        }
    }
    buttonRow {
        button {
            text = "⚙️ Пользовательская"
            type = MoneyManagerButtonType.PERIOD_CUSTOM
        }
    }
}

private fun kz.rmr.chatmachinist.api.reply.KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.customUnitButtons() {
    buttonRow {
        button {
            text = "📆 Дней"
            type = MoneyManagerButtonType.CUSTOM_UNIT_DAYS
        }
    }
    buttonRow {
        button {
            text = "📆 Недель"
            type = MoneyManagerButtonType.CUSTOM_UNIT_WEEKS
        }
    }
    buttonRow {
        button {
            text = "📆 Месяцев"
            type = MoneyManagerButtonType.CUSTOM_UNIT_MONTHS
        }
    }
    buttonRow {
        button {
            text = "📆 Лет"
            type = MoneyManagerButtonType.CUSTOM_UNIT_YEARS
        }
    }
}

private fun kz.rmr.chatmachinist.api.reply.KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.dayOfWeekButtons() {
    buttonRow {
        button {
            text = "Пн"
            type = MoneyManagerButtonType.DAY_MONDAY
        }
        button {
            text = "Вт"
            type = MoneyManagerButtonType.DAY_TUESDAY
        }
    }
    buttonRow {
        button {
            text = "Ср"
            type = MoneyManagerButtonType.DAY_WEDNESDAY
        }
        button {
            text = "Чт"
            type = MoneyManagerButtonType.DAY_THURSDAY
        }
    }
    buttonRow {
        button {
            text = "Пт"
            type = MoneyManagerButtonType.DAY_FRIDAY
        }
        button {
            text = "Сб"
            type = MoneyManagerButtonType.DAY_SATURDAY
        }
    }
    buttonRow {
        button {
            text = "Вс"
            type = MoneyManagerButtonType.DAY_SUNDAY
        }
    }
}

private fun kz.rmr.chatmachinist.api.reply.KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.monthButtons() {
    buttonRow {
        button {
            text = "Янв"
            type = MoneyManagerButtonType.MONTH_JANUARY
        }
        button {
            text = "Фев"
            type = MoneyManagerButtonType.MONTH_FEBRUARY
        }
        button {
            text = "Мар"
            type = MoneyManagerButtonType.MONTH_MARCH
        }
    }
    buttonRow {
        button {
            text = "Апр"
            type = MoneyManagerButtonType.MONTH_APRIL
        }
        button {
            text = "Май"
            type = MoneyManagerButtonType.MONTH_MAY
        }
        button {
            text = "Июн"
            type = MoneyManagerButtonType.MONTH_JUNE
        }
    }
    buttonRow {
        button {
            text = "Июл"
            type = MoneyManagerButtonType.MONTH_JULY
        }
        button {
            text = "Авг"
            type = MoneyManagerButtonType.MONTH_AUGUST
        }
        button {
            text = "Сен"
            type = MoneyManagerButtonType.MONTH_SEPTEMBER
        }
    }
    buttonRow {
        button {
            text = "Окт"
            type = MoneyManagerButtonType.MONTH_OCTOBER
        }
        button {
            text = "Ноя"
            type = MoneyManagerButtonType.MONTH_NOVEMBER
        }
        button {
            text = "Дек"
            type = MoneyManagerButtonType.MONTH_DECEMBER
        }
    }
}