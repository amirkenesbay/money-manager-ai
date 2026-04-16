package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.reply.notification.parseDayOfWeek
import ai.moneymanager.chat.reply.notification.parseMonth
import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationCreateTransitions(
    notificationService: NotificationService
) {
    // ========================
    // STEP 0: Icon input (optional)
    // ========================

    // Valid emoji input (no letters/digits)
    transition {
        name = "Save notification icon"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ICON
            eventType = EventType.TEXT
            guard {
                val text = update.message?.text?.trim() ?: return@guard false
                text.isNotEmpty() && !text.any { it in 'a'..'z' || it in 'A'..'Z' || it in 'а'..'я' || it in 'А'..'Я' || it in '0'..'9' }
            }
        }
        action {
            context.notifIconInput = update.message?.text?.trim()
            context.iconInputError = false
            context.manualTextInputActive = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_ICON
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Invalid input (contains letters/digits)
    transition {
        name = "Reject non-emoji icon input"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ICON
            eventType = EventType.TEXT
        }
        action {
            context.iconInputError = true
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_ICON
        }
    }

    // After valid icon → go to custom name text input (create mode)
    transition {
        name = "Icon saved -> custom name input (create)"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ICON
            eventType = EventType.TRIGGERED
            guard { context.notifIconInput != null && !context.notifEditMode }
        }
        action {
            context.customNameInputMode = true
            context.manualTextInputActive = true
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_NAME
        }
    }

    // After valid icon → go to actions (edit mode)
    transition {
        name = "Icon saved -> actions (edit)"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ICON
            eventType = EventType.TRIGGERED
            guard { context.notifIconInput != null && context.notifEditMode }
        }
        action {
            val id = context.currentNotification?.id ?: return@action
            context.currentNotification = notificationService.updateIcon(id, context.notifIconInput)
            context.notifEditMode = false
            context.manualTextInputActive = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_ACTIONS
        }
    }

    // Skip icon → go to custom name text input
    transition {
        name = "Skip notification icon"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_ICON
            button = MoneyManagerButtonType.NOTIFICATION_SKIP_ICON
        }
        action {
            context.notifIconInput = null
            context.customNameInputMode = true
            context.manualTextInputActive = true
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_NAME
        }
    }

    // Cancel from icon
    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_ICON, notificationService)

    // ========================
    // STEP 1: Name input (quick templates + custom)
    // ========================

    // Quick notification name buttons (icon from template)
    QuickTemplates.NOTIFICATIONS.forEach { template ->
        transition {
            name = "Quick notification name: ${template.name}"
            condition {
                from = MoneyManagerState.NOTIFICATION_CREATE_NAME
                button = template.buttonType
            }
            action {
                context.notifNameInput = template.name
                context.notifIconInput = template.icon
                context.customNameInputMode = false
                context.manualTextInputActive = false
            }
            then {
                to = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
            }
        }
    }

    // Enter custom name → go to icon selection first
    simpleTransitionWithAction(
        "Enter custom notification name -> icon",
        MoneyManagerState.NOTIFICATION_CREATE_NAME,
        MoneyManagerButtonType.ENTER_CUSTOM_NAME,
        MoneyManagerState.NOTIFICATION_CREATE_ICON
    ) {
        context.manualTextInputActive = true
        context.iconInputError = false
    }

    // Custom text input
    transition {
        name = "Save notification name"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_NAME
            eventType = EventType.TEXT
        }
        action {
            val name = update.message?.text?.trim() ?: ""
            if (name.isEmpty() || name.length > 100) {
                context.notifNameInputError = true
            } else {
                context.notifNameInputError = false
                context.notifNameInput = name
                context.manualTextInputActive = false
            }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_NAME
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Name valid -> frequency"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_NAME
            eventType = EventType.TRIGGERED
            guard { context.notifNameInput != null && !context.notifNameInputError }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
        }
    }

    transition {
        name = "Name invalid -> retry"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_NAME
            eventType = EventType.TRIGGERED
            guard { context.notifNameInputError }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_NAME
        }
    }

    // Cancel from name input
    transition {
        name = "Cancel notification creation from name"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_NAME
            button = MoneyManagerButtonType.CANCEL
        }
        action {
            context.clearNotificationInput()
            context.manualTextInputActive = false
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // ========================
    // STEP 2: Frequency selection
    // ========================
    frequencyTransition("Daily", MoneyManagerButtonType.NOTIFICATION_FREQ_DAILY, FrequencyType.DAILY, MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR)
    frequencyTransition("Weekly", MoneyManagerButtonType.NOTIFICATION_FREQ_WEEKLY, FrequencyType.WEEKLY, MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK)
    frequencyTransition("Biweekly", MoneyManagerButtonType.NOTIFICATION_FREQ_BIWEEKLY, FrequencyType.BIWEEKLY, MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK)
    frequencyTransition("Monthly", MoneyManagerButtonType.NOTIFICATION_FREQ_MONTHLY, FrequencyType.MONTHLY, MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH)
    frequencyTransition("Bimonthly", MoneyManagerButtonType.NOTIFICATION_FREQ_BIMONTHLY, FrequencyType.BIMONTHLY, MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH)
    frequencyTransition("Yearly", MoneyManagerButtonType.NOTIFICATION_FREQ_YEARLY, FrequencyType.YEARLY, MoneyManagerState.NOTIFICATION_CREATE_MONTH)

    simpleTransition(
        "Custom frequency",
        MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY,
        MoneyManagerButtonType.NOTIFICATION_FREQ_CUSTOM,
        MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_UNIT
    )

    // Cancel from frequency
    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY, notificationService)

    // ========================
    // STEP 3: Custom unit selection
    // ========================
    customUnitTransition("Custom days", MoneyManagerButtonType.NOTIFICATION_CUSTOM_DAYS, FrequencyType.CUSTOM_DAYS)
    customUnitTransition("Custom weeks", MoneyManagerButtonType.NOTIFICATION_CUSTOM_WEEKS, FrequencyType.CUSTOM_WEEKS)
    customUnitTransition("Custom months", MoneyManagerButtonType.NOTIFICATION_CUSTOM_MONTHS, FrequencyType.CUSTOM_MONTHS)
    customUnitTransition("Custom years", MoneyManagerButtonType.NOTIFICATION_CUSTOM_YEARS, FrequencyType.CUSTOM_YEARS)

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_UNIT, notificationService)

    // ========================
    // STEP 4: Custom N input
    // ========================
    transition {
        name = "Save custom N"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TEXT
        }
        action {
            val text = update.message?.text?.trim() ?: ""
            val n = text.toIntOrNull()
            val max = when (context.notifFrequencyType) {
                FrequencyType.CUSTOM_DAYS -> 365
                FrequencyType.CUSTOM_WEEKS -> 52
                FrequencyType.CUSTOM_MONTHS -> 24
                FrequencyType.CUSTOM_YEARS -> 10
                else -> 365
            }
            if (n == null || n < 1 || n > max) {
                context.notifCustomInputError = true
            } else {
                context.notifCustomInputError = false
                context.notifCustomInterval = n
                context.manualTextInputActive = false
            }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Route based on frequency type after custom N
    transition {
        name = "Custom days -> hour"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TRIGGERED
            guard { context.notifCustomInterval != null && context.notifFrequencyType == FrequencyType.CUSTOM_DAYS }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
        }
    }

    transition {
        name = "Custom weeks -> day of week"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TRIGGERED
            guard { context.notifCustomInterval != null && context.notifFrequencyType == FrequencyType.CUSTOM_WEEKS }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK
        }
    }

    transition {
        name = "Custom months -> day of month"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TRIGGERED
            guard { context.notifCustomInterval != null && context.notifFrequencyType == FrequencyType.CUSTOM_MONTHS }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH
        }
    }

    transition {
        name = "Custom years -> month"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TRIGGERED
            guard { context.notifCustomInterval != null && context.notifFrequencyType == FrequencyType.CUSTOM_YEARS }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_MONTH
        }
    }

    transition {
        name = "Custom N invalid -> retry"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
            eventType = EventType.TRIGGERED
            guard { context.notifCustomInputError }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N, notificationService)

    // ========================
    // STEP 5: Day of week
    // ========================
    transition {
        name = "Select day of week"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK
            button = MoneyManagerButtonType.NOTIFICATION_DAY_OF_WEEK
        }
        action {
            context.notifDayOfWeek = parseDayOfWeek(buttonText ?: "")
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_WEEK, notificationService)

    // ========================
    // STEP 6: Day of month
    // ========================
    transition {
        name = "Select day of month"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH
            button = MoneyManagerButtonType.NOTIFICATION_DAY_OF_MONTH
        }
        action {
            context.notifDayOfMonth = buttonText?.toIntOrNull()
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH, notificationService)

    // ========================
    // STEP 7: Month (for yearly)
    // ========================
    transition {
        name = "Select month"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_MONTH
            button = MoneyManagerButtonType.NOTIFICATION_MONTH_ITEM
        }
        action {
            context.notifMonthOfYear = parseMonth(buttonText ?: "")
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_DAY_OF_MONTH
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_MONTH, notificationService)

    // ========================
    // STEP 8: Hour selection
    // ========================
    transition {
        name = "Select hour"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR
            button = MoneyManagerButtonType.NOTIFICATION_HOUR
        }
        action {
            context.notifHour = buttonText?.toIntOrNull()
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_SELECT_MINUTE
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_SELECT_HOUR, notificationService)

    // ========================
    // STEP 9: Minute selection
    // ========================
    transition {
        name = "Select minute"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_SELECT_MINUTE
            button = MoneyManagerButtonType.NOTIFICATION_MINUTE
        }
        action {
            context.notifMinute = buttonText?.toIntOrNull()
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
        }
    }

    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_SELECT_MINUTE, notificationService)

    // ========================
    // STEP 10: Confirm
    // ========================
    transition {
        name = "Confirm create notification"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
            button = MoneyManagerButtonType.NOTIFICATION_CONFIRM_CREATE
            guard { !context.notifEditMode }
        }
        action {
            val timezone = context.userInfo?.timezone ?: "UTC"
            notificationService.createNotification(
                telegramUserId = user.id,
                name = context.notifNameInput!!,
                icon = context.notifIconInput,
                frequencyType = context.notifFrequencyType!!,
                customInterval = context.notifCustomInterval,
                hour = context.notifHour!!,
                minute = context.notifMinute!!,
                dayOfWeek = context.notifDayOfWeek,
                dayOfMonth = context.notifDayOfMonth,
                monthOfYear = context.notifMonthOfYear,
                userTimezone = timezone
            )
            context.clearNotificationInput()
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    transition {
        name = "Confirm edit notification"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CONFIRM
            button = MoneyManagerButtonType.NOTIFICATION_CONFIRM_CREATE
            guard { context.notifEditMode }
        }
        action {
            val id = context.currentNotification?.id ?: return@action
            val timezone = context.userInfo?.timezone ?: "UTC"
            val updated = notificationService.updateFrequency(
                id = id,
                frequencyType = context.notifFrequencyType!!,
                customInterval = context.notifCustomInterval,
                hour = context.notifHour!!,
                minute = context.notifMinute!!,
                dayOfWeek = context.notifDayOfWeek,
                dayOfMonth = context.notifDayOfMonth,
                monthOfYear = context.notifMonthOfYear,
                userTimezone = timezone
            )
            context.currentNotification = updated
            context.clearNotificationInput()
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_ACTIONS
        }
    }

    // Cancel from confirm
    cancelToList(MoneyManagerState.NOTIFICATION_CREATE_CONFIRM, notificationService)
}

// ========================
// HELPER FUNCTIONS
// ========================

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.frequencyTransition(
    name: String,
    button: MoneyManagerButtonType,
    frequencyType: FrequencyType,
    nextState: MoneyManagerState
) {
    transition {
        this.name = "Select frequency: $name"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
            this.button = button
        }
        action {
            context.notifFrequencyType = frequencyType
        }
        then {
            to = nextState
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.customUnitTransition(
    name: String,
    button: MoneyManagerButtonType,
    frequencyType: FrequencyType
) {
    transition {
        this.name = "Select custom unit: $name"
        condition {
            from = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_UNIT
            this.button = button
        }
        action {
            context.notifFrequencyType = frequencyType
            context.manualTextInputActive = true
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_CUSTOM_N
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.cancelToList(
    fromState: MoneyManagerState,
    notificationService: NotificationService
) {
    transition {
        name = "Cancel from ${fromState.name}"
        condition {
            from = fromState
            button = MoneyManagerButtonType.CANCEL
        }
        action {
            context.clearNotificationInput()
            context.manualTextInputActive = false
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }
}
