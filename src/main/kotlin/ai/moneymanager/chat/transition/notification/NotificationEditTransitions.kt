package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationEditTransitions(
    notificationService: NotificationService
) {
    // Actions -> Edit menu
    simpleTransition(
        "Open edit menu",
        MoneyManagerState.NOTIFICATION_ACTIONS,
        MoneyManagerButtonType.EDIT_NOTIFICATION,
        MoneyManagerState.NOTIFICATION_EDIT_MENU
    )

    // Edit name
    transition {
        name = "Start edit notification name"
        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.NOTIFICATION_EDIT_NAME_BTN
        }
        action {
            context.manualTextInputActive = true
        }
        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_NAME
        }
    }

    transition {
        name = "Save notification name edit"
        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_NAME
            eventType = EventType.TEXT
        }
        action {
            val newName = update.message?.text?.trim()?.take(100) ?: ""
            if (newName.isNotEmpty()) {
                val id = context.currentNotification?.id ?: return@action
                val updated = notificationService.updateName(id, newName)
                context.currentNotification = updated
            }
            context.manualTextInputActive = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_ACTIONS
        }
    }

    transition {
        name = "Cancel edit name"
        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_NAME
            button = MoneyManagerButtonType.CANCEL
        }
        action {
            context.manualTextInputActive = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_EDIT_MENU
        }
    }

    // Edit icon -> reuses create icon state
    transition {
        name = "Start edit notification icon"
        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.NOTIFICATION_EDIT_ICON_BTN
        }
        action {
            context.notifEditMode = true
            context.manualTextInputActive = true
            context.iconInputError = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_ICON
        }
    }

    // Edit frequency -> reuses create flow
    transition {
        name = "Start edit frequency"
        condition {
            from = MoneyManagerState.NOTIFICATION_EDIT_MENU
            button = MoneyManagerButtonType.NOTIFICATION_EDIT_FREQUENCY_BTN
        }
        action {
            context.notifEditMode = true
            context.notifNameInput = context.currentNotification?.name
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_FREQUENCY
        }
    }

    // Back from edit menu to actions
    simpleTransition(
        "Back from edit menu",
        MoneyManagerState.NOTIFICATION_EDIT_MENU,
        MoneyManagerButtonType.BACK_TO_NOTIFICATIONS,
        MoneyManagerState.NOTIFICATION_ACTIONS
    )

    // Toggle active/pause
    transition {
        name = "Toggle notification"
        condition {
            from = MoneyManagerState.NOTIFICATION_ACTIONS
            button = MoneyManagerButtonType.TOGGLE_NOTIFICATION
        }
        action {
            val id = context.currentNotification?.id ?: return@action
            val timezone = context.userInfo?.timezone ?: "UTC"
            context.currentNotification = notificationService.toggleNotification(id, timezone)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_ACTIONS
        }
    }
}
