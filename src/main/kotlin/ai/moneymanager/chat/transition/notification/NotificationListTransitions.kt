package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationListTransitions(
    notificationService: NotificationService
) {
    transition {
        name = "Select notification from list"
        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.NOTIFICATION_ITEM
        }
        action {
            val text = buttonText ?: return@action
            context.currentNotification = context.notifications.find { n ->
                text.endsWith(n.name)
            }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_ACTIONS
        }
    }

    transition {
        name = "Start create notification"
        condition {
            from = MoneyManagerState.NOTIFICATION_LIST
            button = MoneyManagerButtonType.CREATE_NOTIFICATION
        }
        action {
            context.clearNotificationInput()
            context.customNameInputMode = false
        }
        then {
            to = MoneyManagerState.NOTIFICATION_CREATE_NAME
        }
    }

    transition {
        name = "Back to notification list from actions"
        condition {
            from = MoneyManagerState.NOTIFICATION_ACTIONS
            button = MoneyManagerButtonType.BACK_TO_NOTIFICATIONS
        }
        action {
            context.notifications = notificationService.getNotifications(user.id)
            context.currentNotification = null
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    simpleTransition(
        "Back to menu from notifications",
        MoneyManagerState.NOTIFICATION_LIST,
        MoneyManagerButtonType.BACK_TO_MENU,
        MoneyManagerState.MENU
    )
}
