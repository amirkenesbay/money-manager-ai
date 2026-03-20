package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.editGroupTransitions(
    groupService: GroupService
) {
    transition {
        name = "Update group name"

        condition {
            from = MoneyManagerState.GROUP_EDIT_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            context.manualTextInputActive = false
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@action
            val newName = update.message.text ?: return@action

            val updatedGroup = groupService.updateGroupName(userId, groupId, newName)
            if (updatedGroup != null) {
                context.currentGroup = updatedGroup
                context.userGroups = groupService.getUserGroups(userId)
            }
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }

    transition {
        name = "Cancel edit name"

        condition {
            from = MoneyManagerState.GROUP_EDIT_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }
}