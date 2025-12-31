package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.moneyManagerDialog(
    userInfoService: UserInfoService,
    groupService: GroupService
) {
    dialog {
        name = "Money Manager Dialog"

        startMoneyManagerDialogTransition(userInfoService, groupService)
        joinGroupDialogTransitions(groupService)
        groupDialogTransitions(groupService)
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.startMoneyManagerDialogTransition(
    userInfoService: UserInfoService,
    groupService: GroupService
) {
    // Обработка команды /start
    transition {
        name = "Start Money Manager Dialog"
        startDialog = true

        condition {
            eventType = EventType.COMMAND

            guard {
                update.message?.text?.startsWith("/start") == true
            }
        }

        action {
            context.userInfo = userInfoService.getUserInfo(user)

            // Проверяем, есть ли параметр deep link для присоединения к группе
            val messageText = update.message?.text
            if (messageText != null) {
                val parts = messageText.split(" ")
                if (parts.size == 2 && parts[1].startsWith("join_")) {
                    val token = parts[1].removePrefix("join_")
                    context.pendingInviteToken = token
                    context.pendingGroup = groupService.getGroupByToken(token)
                }
            }
        }

        then {
            to = MoneyManagerState.STARTED

            noReply = true

            trigger {
                sameDialog = true
            }
        }
    }

    // Обработка команды /join
    transition {
        name = "Join command"

        condition {
            eventType = EventType.COMMAND

            guard {
                update.message?.text?.startsWith("/join") == true
            }
        }

        action {
            context.userInfo = userInfoService.getUserInfo(user)

            // Проверяем, есть ли токен после команды /join
            val messageText = update.message?.text
            if (messageText != null) {
                val parts = messageText.split(" ")
                if (parts.size == 2) {
                    val token = parts[1]
                    context.pendingInviteToken = token
                    context.pendingGroup = groupService.getGroupByToken(token)
                }
            }
        }

        then {
            to = MoneyManagerState.STARTED

            noReply = true

            trigger {
                sameDialog = true
            }
        }
    }

    // Если есть приглашение в группу, показываем подтверждение
    transition {
        name = "Show group join confirmation"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup != null
            }
        }

        then {
            to = MoneyManagerState.GROUP_JOIN_CONFIRM
        }
    }

    // Иначе переходим в главное меню
    transition {
        name = "Open main menu"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null
            }
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.joinGroupDialogTransitions(
    groupService: GroupService
) {
    // Подтверждение присоединения к группе
    transition {
        name = "Confirm join group"

        condition {
            from = MoneyManagerState.GROUP_JOIN_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_JOIN
        }

        action {
            val userId = user.id
            val token = context.pendingInviteToken
            if (token != null) {
                val joinedGroup = groupService.joinGroup(userId, token)
                context.currentGroup = joinedGroup
            }
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    // Отклонение приглашения
    transition {
        name = "Cancel join group"

        condition {
            from = MoneyManagerState.GROUP_JOIN_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            // Очищаем данные о приглашении
            context.pendingInviteToken = null
            context.pendingGroup = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}