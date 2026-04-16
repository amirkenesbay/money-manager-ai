package ai.moneymanager.chat.transition.notification

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationDialogTransitions(
    notificationService: NotificationService,
    userInfoService: UserInfoService
) {
    notificationTimezoneTransitions(userInfoService, notificationService)
    notificationListTransitions(notificationService)
    notificationCreateTransitions(notificationService)
    notificationEditTransitions(notificationService)
    notificationDeleteTransitions(notificationService)
}
