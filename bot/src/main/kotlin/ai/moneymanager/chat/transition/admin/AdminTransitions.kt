package ai.moneymanager.chat.transition.admin

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType

private const val ACTIVATE_COMMAND = "/activate"
private const val ACTIVATE_USAGE = "Использование: /activate <username или telegramId> <дней>"
private const val ACTIVATE_USER_NOT_FOUND = "Пользователь не найден: %s"
private const val ACTIVATE_SUCCESS = "✅ Подписка активирована для %s до %s"

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.adminDialogTransitions(
    adminService: AdminService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Activate subscription (admin)"

        listOf(MoneyManagerState.MENU, MoneyManagerState.MINI_APP_GATEWAY).forEach { state ->
            condition {
                from = state
                eventType = EventType.COMMAND
                guard { isAdminActivation(adminService) }
            }
        }

        action {
            val chatId = update.message?.chatId ?: return@action
            val args = update.message?.text.orEmpty().split(" ").drop(1)
            val identifier = args.getOrNull(0)
            val days = args.getOrNull(1)?.toLongOrNull()

            if (identifier == null || days == null || days <= 0) {
                adminService.reply(chatId, ACTIVATE_USAGE)
                return@action
            }

            val target = userInfoService.findUserByUsernameOrTelegramId(identifier)
            val targetTelegramId = target?.telegramUserId
            if (target == null || targetTelegramId == null) {
                adminService.reply(chatId, ACTIVATE_USER_NOT_FOUND.format(identifier))
                return@action
            }

            val updated = userInfoService.activateSubscription(targetTelegramId, days)
            val expiresAt = updated?.subscriptionExpiresAt
            if (expiresAt != null) {
                val displayName = target.username?.let { "@$it" } ?: identifier
                adminService.reply(chatId, ACTIVATE_SUCCESS.format(displayName, expiresAt.toLocalDate()))
            }
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
        }
    }
}

fun ActionContext<MoneyManagerState, MoneyManagerContext>.isAdminActivation(adminService: AdminService): Boolean =
    update.message?.text?.startsWith(ACTIVATE_COMMAND) == true && adminService.isAdmin(user.id)
