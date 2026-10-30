package ai.moneymanager.chat.transition.gateway

import ai.moneymanager.chat.state.MoneyManagerButtonType
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.transition.admin.isAdminActivation
import ai.moneymanager.chat.transition.common.inviteTokenOf
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.api.transition.ThenBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType

private const val GATEWAY_DIALOG_NAME = "Mini App Gateway Dialog"
private val MESSAGE_EVENTS = listOf(EventType.TEXT, EventType.COMMAND, EventType.VOICE, EventType.PHOTO, EventType.DOCUMENT)
private val PASS_THROUGH_BUTTONS = setOf(MoneyManagerButtonType.CONFIRM_JOIN)

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.miniAppGatewayDialog(
    gateway: MiniAppGateway,
    userInfoService: UserInfoService,
    adminService: AdminService
) {
    if (!gateway.enabled) return
    dialog {
        name = GATEWAY_DIALOG_NAME
        redirectToMiniAppTransition(gateway, userInfoService, adminService)
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.miniAppHintTransitions(gateway: MiniAppGateway) {
    if (!gateway.enabled) return
    showMiniAppHintTransition(gateway)
}

fun ThenBuilder<MoneyManagerState, MoneyManagerContext>.toHome(gateway: MiniAppGateway) {
    toGatewayOrMenu(gateway)
    if (gateway.enabled) trigger { sameDialog = true }
}

fun ThenBuilder<MoneyManagerState, MoneyManagerContext>.toGatewayOrMenu(gateway: MiniAppGateway) {
    if (!gateway.enabled) {
        to = MoneyManagerState.MENU
        return
    }
    to = MoneyManagerState.MINI_APP_GATEWAY
    noReply = true
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.redirectToMiniAppTransition(
    gateway: MiniAppGateway,
    userInfoService: UserInfoService,
    adminService: AdminService
) {
    transition {
        name = "Redirect to Mini App"
        startDialog = true

        condition {
            eventTypes = MESSAGE_EVENTS
            guard { isOnboarded(userInfoService) && !isPassThroughMessage(adminService) }
        }

        MoneyManagerButtonType.entries.filter { it !in PASS_THROUGH_BUTTONS }.forEach { buttonType ->
            condition {
                eventType = EventType.INLINE_BUTTON_CLICKED
                button = buttonType
                guard { isOnboarded(userInfoService) }
            }
        }

        action {
            gateway.sendHint(chat.id, user.id)
        }

        then {
            toGatewayOrMenu(gateway)
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.showMiniAppHintTransition(gateway: MiniAppGateway) {
    transition {
        name = "Show Mini App hint"

        condition {
            from = MoneyManagerState.MINI_APP_GATEWAY
            eventType = EventType.TRIGGERED
        }

        action {
            gateway.sendHint(chat.id, user.id)
        }

        then {
            toGatewayOrMenu(gateway)
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.isOnboarded(userInfoService: UserInfoService): Boolean =
    userInfoService.getUserInfoByTelegramId(user.id)?.onboardingCompleted == true

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.isPassThroughMessage(adminService: AdminService): Boolean =
    inviteTokenOf(update.message?.text) != null || isAdminActivation(adminService)
