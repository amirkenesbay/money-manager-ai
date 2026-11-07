package ai.moneymanager.chat.transition.access

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.model.EventType

private const val BLOCKED_DIALOG_NAME = "Blocked User Dialog"
private const val BLOCKED_MESSAGE_KEY = "user.blocked"
private val BLOCKED_USER_EVENTS = listOf(
    EventType.TEXT, EventType.COMMAND, EventType.VOICE, EventType.PHOTO, EventType.DOCUMENT, EventType.INLINE_BUTTON_CLICKED,
)

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.blockedUserDialog(
    userInfoService: UserInfoService,
    localizationService: LocalizationService,
    adminService: AdminService,
) {
    dialog {
        name = BLOCKED_DIALOG_NAME
        transition {
            name = "Tell a blocked user about the block"
            startDialog = true

            condition {
                eventTypes = BLOCKED_USER_EVENTS
                guard { !adminService.isAdmin(user.id) && userInfoService.getUserInfoByTelegramId(user.id)?.blocked == true }
            }

            action {
                val language = userInfoService.getUserInfoByTelegramId(user.id)?.language
                adminService.reply(chat.id, localizationService.t(BLOCKED_MESSAGE_KEY, language))
            }

            then {
                to = MoneyManagerState.MENU
                noReply = true
            }
        }
    }
}
