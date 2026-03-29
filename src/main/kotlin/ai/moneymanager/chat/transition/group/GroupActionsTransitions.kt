package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.parseGroupNameFromButton
import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupActionsTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Select group from list"
        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.GROUP_ITEM
        }
        action {
            val groupName = parseGroupNameFromButton(buttonText ?: return@action)
            context.currentGroup = context.userGroups.find { it.name == groupName }
        }
        then { to = MoneyManagerState.GROUP_ACTIONS }
    }

    simpleTransitionWithAction("Show invite link from actions",
        MoneyManagerState.GROUP_ACTIONS, MoneyManagerButtonType.INVITE_TO_GROUP, MoneyManagerState.GROUP_INVITE_SHOW
    ) {
        context.isQuickGroupCreation = false
        context.inviteFromActions = true
    }

    simpleTransitionWithAction("View group members from actions",
        MoneyManagerState.GROUP_ACTIONS, MoneyManagerButtonType.GROUP_MEMBERS, MoneyManagerState.GROUP_MEMBERS
    ) {
        val group = context.currentGroup ?: return@simpleTransitionWithAction
        context.groupMembersList = group.memberIds.mapNotNull { userInfoService.getUserInfoByTelegramId(it) }
    }

    simpleTransitionWithAction("Switch active group from actions",
        MoneyManagerState.GROUP_ACTIONS, MoneyManagerButtonType.SWITCH_GROUP, MoneyManagerState.GROUP_ACTIONS
    ) {
        val groupId = context.currentGroup?.id ?: return@simpleTransitionWithAction
        groupService.switchActiveGroup(user.id, groupId)
        context.userInfo = context.userInfo?.copy(activeGroupId = groupId)
        context.userGroups = groupService.getUserGroups(user.id)
    }

    simpleTransition("Back to group list from actions",
        MoneyManagerState.GROUP_ACTIONS, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.GROUP_LIST)
}
