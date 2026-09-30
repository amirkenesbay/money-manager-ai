package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationDialogTransitions(
    notificationService: NotificationService,
    userInfoService: UserInfoService,
    subscriptionLimitsService: SubscriptionLimitsService
) {
    notificationTimezoneTransitions(userInfoService, notificationService)
    notificationListTransitions(notificationService)
    notificationCreateTransitions(notificationService, subscriptionLimitsService)
    notificationEditTransitions(notificationService)
    notificationDeleteTransitions(notificationService)
}
