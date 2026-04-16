package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.transition.common.confirmFlow
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationDeleteTransitions(
    notificationService: NotificationService
) {
    // Delete single notification
    confirmFlow(
        flowName = "delete notification",
        sourceState = MoneyManagerState.NOTIFICATION_ACTIONS,
        confirmState = MoneyManagerState.NOTIFICATION_DELETE_CONFIRM,
        returnState = MoneyManagerState.NOTIFICATION_LIST,
        triggerButton = MoneyManagerButtonType.DELETE_NOTIFICATION,
        cancelState = MoneyManagerState.NOTIFICATION_ACTIONS,
        onConfirm = {
            val id = context.currentNotification?.id ?: return@confirmFlow
            notificationService.deleteNotification(id)
            context.currentNotification = null
            context.notifications = notificationService.getNotifications(user.id)
        }
    )

    // Delete all notifications
    confirmFlow(
        flowName = "delete all notifications",
        sourceState = MoneyManagerState.NOTIFICATION_LIST,
        confirmState = MoneyManagerState.NOTIFICATION_DELETE_ALL_CONFIRM,
        returnState = MoneyManagerState.NOTIFICATION_LIST,
        triggerButton = MoneyManagerButtonType.DELETE_ALL_NOTIFICATIONS,
        onConfirm = {
            notificationService.deleteAllNotifications(user.id)
            context.notifications = emptyList()
            context.currentNotification = null
        }
    )
}
