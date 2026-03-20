package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteGroupTransitions(
    groupService: GroupService
) {
    transition {
        name = "Confirm delete group"

        condition {
            from = MoneyManagerState.GROUP_DELETE_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@action

            val deleted = groupService.deleteGroup(userId, groupId)
            if (deleted) {
                val groups = groupService.getUserGroups(userId)
                context.userGroups = groups

                val activeGroup = groupService.getActiveGroup(userId)
                context.userInfo = context.userInfo?.copy(
                    activeGroupId = activeGroup?.id,
                    groupIds = groups.mapNotNull { it.id }.toSet()
                )

                context.currentGroup = null
            }
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    transition {
        name = "Cancel delete group"

        condition {
            from = MoneyManagerState.GROUP_DELETE_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }
}