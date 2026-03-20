package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewGroupsListTransitions(
    groupService: GroupService
) {
    transition {
        name = "View groups list"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.MY_GROUPS
        }

        action {
            val userId = user.id
            val groups = groupService.getUserGroups(userId)
            context.userGroups = groups

            val activeGroup = groupService.getActiveGroup(userId)
            context.userInfo = context.userInfo?.copy(
                activeGroupId = activeGroup?.id,
                groupIds = groups.mapNotNull { it.id }.toSet()
            )
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }
}