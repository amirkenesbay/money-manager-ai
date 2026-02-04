package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.NotificationCustomUnit
import ai.moneymanager.domain.model.NotificationPeriodType
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationDialogTransitions(
    notificationService: NotificationService
) {
    // Открыть список уведомлений
    openNotificationListTransition(notificationService)

    // Создание уведомления
    createNotificationDialogTransitions(notificationService)

    // Редактирование уведомления
    editNotificationDialogTransitions(notificationService)

    // Удаление уведомления
    deleteNotificationDialogTransitions(notificationService)

    // Возврат в главное меню
    backToMenuFromNotificationsTransition()
}

/**
 * Открыть список уведомлений
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openNotificationListTransition(
    notificationService: NotificationService
) {
    transition {
        name = "Open notification list"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.NOTIFICATIONS
        }

        action {
            val userId = user.id
            context.notifications = notificationService.getNotificationsByUserId(userId)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }
}

/**
 * Диалог создания уведомления
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createNotificationDialogTransitions(
    notificationService: NotificationService
) {
    // Начать создание уведомления
    transition {
        name = "Start notification creation"

        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.CREATE_NOTIFICATION
        }

        action {
            // Очищаем временные данные
            clearNotificationInputs(context)
            context.isEditingNotification = false
        }

        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_ENTER_NAME
        }
    }

    // Ввод названия
    transition {
        name = "Enter notification name"

        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val name = update.message.text ?: ""
            context.notificationTitleInput = name.take(50)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_SELECT_PERIOD
        }
    }

    // Отмена ввода названия
    transition {
        name = "Cancel notification name input"

        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            clearNotificationInputs(context)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Выбор периодичности
    createPeriodSelectionTransitions(isEditing = false)

    // Ввод времени и подтверждение создания
    createTimeInputTransitions(notificationService, isEditing = false)
}

/**
 * Переходы для выбора периодичности (используется и при создании, и при редактировании)
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createPeriodSelectionTransitions(isEditing: Boolean) {
    val selectPeriodState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_SELECT_PERIOD else MoneyManagerState.NOTIFICATION_CREATE_SELECT_PERIOD
    val customEnterNState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_CUSTOM_ENTER_N else MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_ENTER_N
    val customSelectUnitState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_CUSTOM_SELECT_UNIT else MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_SELECT_UNIT
    val selectDayOfWeekState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_SELECT_DAY_OF_WEEK else MoneyManagerState.NOTIFICATION_CREATE_SELECT_DAY_OF_WEEK
    val enterDayOfMonthState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_ENTER_DAY_OF_MONTH else MoneyManagerState.NOTIFICATION_CREATE_ENTER_DAY_OF_MONTH
    val selectMonthState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_SELECT_MONTH else MoneyManagerState.NOTIFICATION_CREATE_SELECT_MONTH
    val enterTimeState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_ENTER_TIME else MoneyManagerState.NOTIFICATION_CREATE_ENTER_TIME
    val previousState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_MENU else MoneyManagerState.NOTIFICATION_CREATE_ENTER_NAME
    val prefix = if (isEditing) "Edit" else "Create"

    // Каждый день -> ввод времени
    transition {
        name = "$prefix: Select daily period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_DAILY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.DAILY
        }

        then {
            to = enterTimeState
        }
    }

    // Каждую неделю -> выбор дня недели
    transition {
        name = "$prefix: Select weekly period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_WEEKLY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.WEEKLY
        }

        then {
            to = selectDayOfWeekState
        }
    }

    // Каждые 2 недели -> выбор дня недели
    transition {
        name = "$prefix: Select biweekly period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_BIWEEKLY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.BIWEEKLY
        }

        then {
            to = selectDayOfWeekState
        }
    }

    // Каждый месяц -> ввод дня месяца
    transition {
        name = "$prefix: Select monthly period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_MONTHLY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.MONTHLY
        }

        then {
            to = enterDayOfMonthState
        }
    }

    // Каждые 2 месяца -> ввод дня месяца
    transition {
        name = "$prefix: Select bimonthly period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_BIMONTHLY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.BIMONTHLY
        }

        then {
            to = enterDayOfMonthState
        }
    }

    // Каждый год -> выбор месяца
    transition {
        name = "$prefix: Select yearly period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_YEARLY
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.YEARLY
        }

        then {
            to = selectMonthState
        }
    }

    // Пользовательская -> ввод числа N
    transition {
        name = "$prefix: Select custom period"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.PERIOD_CUSTOM
        }

        action {
            context.notificationPeriodTypeInput = NotificationPeriodType.CUSTOM
        }

        then {
            to = customEnterNState
        }
    }

    // Отмена выбора периода
    transition {
        name = "$prefix: Cancel period selection"

        condition {
            from = selectPeriodState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = previousState
        }
    }

    // ----- Custom period flow -----

    // Ввод числа N
    transition {
        name = "$prefix: Enter custom N"

        condition {
            from = customEnterNState
            eventType = EventType.TEXT
        }

        action {
            val n = update.message.text?.toIntOrNull()?.coerceIn(1, 365) ?: 1
            context.notificationCustomNInput = n
        }

        then {
            to = customSelectUnitState
        }
    }

    // Отмена ввода числа N
    transition {
        name = "$prefix: Cancel custom N input"

        condition {
            from = customEnterNState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = selectPeriodState
        }
    }

    // Выбор единицы: дни -> ввод времени
    transition {
        name = "$prefix: Select custom unit days"

        condition {
            from = customSelectUnitState
            button = MoneyManagerButtonType.CUSTOM_UNIT_DAYS
        }

        action {
            context.notificationCustomUnitInput = NotificationCustomUnit.DAYS
        }

        then {
            to = enterTimeState
        }
    }

    // Выбор единицы: недели -> выбор дня недели
    transition {
        name = "$prefix: Select custom unit weeks"

        condition {
            from = customSelectUnitState
            button = MoneyManagerButtonType.CUSTOM_UNIT_WEEKS
        }

        action {
            context.notificationCustomUnitInput = NotificationCustomUnit.WEEKS
        }

        then {
            to = selectDayOfWeekState
        }
    }

    // Выбор единицы: месяцы -> ввод дня месяца
    transition {
        name = "$prefix: Select custom unit months"

        condition {
            from = customSelectUnitState
            button = MoneyManagerButtonType.CUSTOM_UNIT_MONTHS
        }

        action {
            context.notificationCustomUnitInput = NotificationCustomUnit.MONTHS
        }

        then {
            to = enterDayOfMonthState
        }
    }

    // Выбор единицы: годы -> выбор месяца
    transition {
        name = "$prefix: Select custom unit years"

        condition {
            from = customSelectUnitState
            button = MoneyManagerButtonType.CUSTOM_UNIT_YEARS
        }

        action {
            context.notificationCustomUnitInput = NotificationCustomUnit.YEARS
        }

        then {
            to = selectMonthState
        }
    }

    // Отмена выбора единицы
    transition {
        name = "$prefix: Cancel custom unit selection"

        condition {
            from = customSelectUnitState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = customEnterNState
        }
    }

    // ----- Day of week selection -----
    val dayButtons = listOf(
        MoneyManagerButtonType.DAY_MONDAY to 1,
        MoneyManagerButtonType.DAY_TUESDAY to 2,
        MoneyManagerButtonType.DAY_WEDNESDAY to 3,
        MoneyManagerButtonType.DAY_THURSDAY to 4,
        MoneyManagerButtonType.DAY_FRIDAY to 5,
        MoneyManagerButtonType.DAY_SATURDAY to 6,
        MoneyManagerButtonType.DAY_SUNDAY to 7
    )

    dayButtons.forEach { (buttonType, dayOfWeek) ->
        transition {
            name = "$prefix: Select day of week $dayOfWeek"

            condition {
                from = selectDayOfWeekState
                button = buttonType
            }

            action {
                context.notificationDayOfWeekInput = dayOfWeek
            }

            then {
                to = enterTimeState
            }
        }
    }

    // Отмена выбора дня недели
    transition {
        name = "$prefix: Cancel day of week selection"

        condition {
            from = selectDayOfWeekState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = selectPeriodState
        }
    }

    // ----- Day of month input -----
    transition {
        name = "$prefix: Enter day of month"

        condition {
            from = enterDayOfMonthState
            eventType = EventType.TEXT
        }

        action {
            val day = update.message.text?.toIntOrNull()?.coerceIn(1, 28) ?: 1
            context.notificationDayOfMonthInput = day
        }

        then {
            to = enterTimeState
        }
    }

    // Отмена ввода дня месяца
    transition {
        name = "$prefix: Cancel day of month input"

        condition {
            from = enterDayOfMonthState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = selectPeriodState
        }
    }

    // ----- Month selection -----
    val monthButtons = listOf(
        MoneyManagerButtonType.MONTH_JANUARY to 1,
        MoneyManagerButtonType.MONTH_FEBRUARY to 2,
        MoneyManagerButtonType.MONTH_MARCH to 3,
        MoneyManagerButtonType.MONTH_APRIL to 4,
        MoneyManagerButtonType.MONTH_MAY to 5,
        MoneyManagerButtonType.MONTH_JUNE to 6,
        MoneyManagerButtonType.MONTH_JULY to 7,
        MoneyManagerButtonType.MONTH_AUGUST to 8,
        MoneyManagerButtonType.MONTH_SEPTEMBER to 9,
        MoneyManagerButtonType.MONTH_OCTOBER to 10,
        MoneyManagerButtonType.MONTH_NOVEMBER to 11,
        MoneyManagerButtonType.MONTH_DECEMBER to 12
    )

    monthButtons.forEach { (buttonType, month) ->
        transition {
            name = "$prefix: Select month $month"

            condition {
                from = selectMonthState
                button = buttonType
            }

            action {
                context.notificationMonthInput = month
            }

            then {
                to = enterDayOfMonthState
            }
        }
    }

    // Отмена выбора месяца
    transition {
        name = "$prefix: Cancel month selection"

        condition {
            from = selectMonthState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = selectPeriodState
        }
    }
}

/**
 * Переходы для ввода времени и подтверждения
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createTimeInputTransitions(
    notificationService: NotificationService,
    isEditing: Boolean
) {
    val enterTimeState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_ENTER_TIME else MoneyManagerState.NOTIFICATION_CREATE_ENTER_TIME
    val confirmState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_CONFIRM else MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
    val listState = MoneyManagerState.NOTIFICATION_LIST
    val selectPeriodState = if (isEditing) MoneyManagerState.NOTIFICATION_EDIT_SELECT_PERIOD else MoneyManagerState.NOTIFICATION_CREATE_SELECT_PERIOD
    val prefix = if (isEditing) "Edit" else "Create"

    // Ввод времени (формат HH:MM)
    transition {
        name = "$prefix: Enter time"

        condition {
            from = enterTimeState
            eventType = EventType.TEXT
        }

        action {
            val text = update.message.text ?: "12:00"
            val parts = text.split(":", ".", " ")
            val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
            val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            context.notificationHourInput = hour
            context.notificationMinuteInput = minute
        }

        then {
            to = confirmState
        }
    }

    // Отмена ввода времени
    transition {
        name = "$prefix: Cancel time input"

        condition {
            from = enterTimeState
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = selectPeriodState
        }
    }

    if (!isEditing) {
        // Подтверждение создания
        transition {
            name = "Confirm notification creation"

            condition {
                from = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
                button = MoneyManagerButtonType.CONFIRM_CREATE_NOTIFICATION
            }

            action {
                val userId = user.id
                val title = context.notificationTitleInput ?: "Напоминание"
                val periodType = context.notificationPeriodTypeInput ?: NotificationPeriodType.DAILY
                val hour = context.notificationHourInput ?: 12
                val minute = context.notificationMinuteInput ?: 0

                notificationService.createNotification(
                    userId = userId,
                    title = title,
                    periodType = periodType,
                    hour = hour,
                    minute = minute,
                    customN = context.notificationCustomNInput,
                    customUnit = context.notificationCustomUnitInput,
                    dayOfWeek = context.notificationDayOfWeekInput,
                    dayOfMonth = context.notificationDayOfMonthInput,
                    month = context.notificationMonthInput
                )

                // Обновляем список
                context.notifications = notificationService.getNotificationsByUserId(userId)
                clearNotificationInputs(context)
            }

            then {
                to = listState
            }
        }

        // Отмена создания
        transition {
            name = "Cancel notification creation"

            condition {
                from = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
                button = MoneyManagerButtonType.CANCEL
            }

            action {
                clearNotificationInputs(context)
            }

            then {
                to = listState
            }
        }
    }
}

/**
 * Диалог редактирования уведомления
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.editNotificationDialogTransitions(
    notificationService: NotificationService
) {
    // Открыть выбор уведомления для редактирования
    transition {
        name = "Open edit notification select"

        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.EDIT_NOTIFICATION
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_SELECT
        }
    }

    // Выбрать уведомление для редактирования
    transition {
        name = "Select notification to edit"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_SELECT
            button = MoneyManagerButtonType.NOTIFICATION_ITEM
        }

        action {
            val buttonText = buttonText ?: return@action
            val notificationNumber = buttonText.substringBefore(".").trim().toIntOrNull() ?: return@action
            val notificationIndex = notificationNumber - 1

            if (notificationIndex >= 0 && notificationIndex < context.notifications.size) {
                context.currentNotification = context.notifications[notificationIndex]
                context.isEditingNotification = true
            }
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }

    // Отмена выбора для редактирования
    transition {
        name = "Cancel edit select"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_SELECT
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Редактировать название
    transition {
        name = "Edit notification name"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.EDIT_NOTIFICATION_NAME
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_ENTER_NAME
        }
    }

    // Ввод нового названия
    transition {
        name = "Enter new notification name"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val newName = update.message.text?.take(50) ?: return@action
            val notificationId = context.currentNotification?.id ?: return@action

            val updated = notificationService.updateNotificationTitle(notificationId, newName)
            if (updated != null) {
                context.currentNotification = updated
                context.notifications = notificationService.getNotificationsByUserId(user.id)
            }
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }

    // Отмена редактирования названия
    transition {
        name = "Cancel edit name"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }

    // Редактировать периодичность
    transition {
        name = "Edit notification period"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.EDIT_NOTIFICATION_PERIOD
        }

        action {
            clearNotificationInputs(context)
            context.isEditingNotification = true
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_SELECT_PERIOD
        }
    }

    // Назад из меню редактирования
    transition {
        name = "Back from edit menu"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        action {
            context.currentNotification = null
            context.isEditingNotification = false
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Переходы для редактирования периодичности
    createPeriodSelectionTransitions(isEditing = true)
    createTimeInputTransitions(notificationService, isEditing = true)

    // Подтверждение редактирования периодичности
    transition {
        name = "Confirm edit notification period"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_EDIT_NOTIFICATION
        }

        action {
            val notificationId = context.currentNotification?.id ?: return@action
            val periodType = context.notificationPeriodTypeInput ?: NotificationPeriodType.DAILY
            val hour = context.notificationHourInput ?: 12
            val minute = context.notificationMinuteInput ?: 0

            val updated = notificationService.updateNotificationPeriod(
                notificationId = notificationId,
                periodType = periodType,
                hour = hour,
                minute = minute,
                customN = context.notificationCustomNInput,
                customUnit = context.notificationCustomUnitInput,
                dayOfWeek = context.notificationDayOfWeekInput,
                dayOfMonth = context.notificationDayOfMonthInput,
                month = context.notificationMonthInput
            )

            if (updated != null) {
                context.currentNotification = updated
                context.notifications = notificationService.getNotificationsByUserId(user.id)
            }
            clearNotificationInputs(context)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }

    // Отмена редактирования периодичности
    transition {
        name = "Cancel edit period"

        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            clearNotificationInputs(context)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }
}

/**
 * Диалог удаления уведомления
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteNotificationDialogTransitions(
    notificationService: NotificationService
) {
    // Открыть выбор уведомления для удаления
    transition {
        name = "Open delete notification select"

        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.DELETE_NOTIFICATION
        }

        then {
            to = MoneyManagerState.NOTIFICATION_DELETE_SELECT
        }
    }

    // Выбрать уведомление для удаления
    transition {
        name = "Select notification to delete"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_SELECT
            button = MoneyManagerButtonType.NOTIFICATION_ITEM
        }

        action {
            val buttonText = buttonText ?: return@action
            val notificationNumber = buttonText.substringBefore(".").trim().toIntOrNull() ?: return@action
            val notificationIndex = notificationNumber - 1

            if (notificationIndex >= 0 && notificationIndex < context.notifications.size) {
                context.currentNotification = context.notifications[notificationIndex]
            }
        }

        then {
            to = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM
        }
    }

    // Отмена выбора для удаления
    transition {
        name = "Cancel delete select"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_SELECT
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Подтверждение удаления одного уведомления
    transition {
        name = "Confirm delete notification"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val notificationId = context.currentNotification?.id ?: return@action
            notificationService.deleteNotification(notificationId)
            context.currentNotification = null
            context.notifications = notificationService.getNotificationsByUserId(user.id)
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Отмена удаления одного уведомления
    transition {
        name = "Cancel delete notification"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.currentNotification = null
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Открыть подтверждение удаления всех
    transition {
        name = "Open delete all notifications confirm"

        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.DELETE_ALL_NOTIFICATIONS
        }

        then {
            to = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM
        }
    }

    // Подтверждение удаления всех
    transition {
        name = "Confirm delete all notifications"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val userId = user.id
            notificationService.deleteAllNotificationsForUser(userId)
            context.notifications = emptyList()
            context.currentNotification = null
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Отмена удаления всех
    transition {
        name = "Cancel delete all notifications"

        condition {
            from = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }
}

/**
 * Возврат в главное меню
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.backToMenuFromNotificationsTransition() {
    transition {
        name = "Back to menu from notification list"

        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        action {
            clearNotificationInputs(context)
            context.notifications = emptyList()
            context.currentNotification = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

/**
 * Очистить временные данные уведомления
 */
private fun clearNotificationInputs(context: MoneyManagerContext) {
    context.notificationTitleInput = null
    context.notificationPeriodTypeInput = null
    context.notificationCustomNInput = null
    context.notificationCustomUnitInput = null
    context.notificationDayOfWeekInput = null
    context.notificationDayOfMonthInput = null
    context.notificationMonthInput = null
    context.notificationHourInput = null
    context.notificationMinuteInput = null
}