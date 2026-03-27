package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupActionsTransitions(
    groupService: GroupService,
    categoryService: CategoryService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Select group from list"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.GROUP_ITEM
        }

        action {
            val buttonText = buttonText ?: return@action
            val groupName = buttonText
                .removePrefix("✅ ")
                .removePrefix("👑 ")
                .removePrefix("✅ 👑 ")
                .trim()

            val selectedGroup = context.userGroups.find { it.name == groupName }
            if (selectedGroup != null) {
                context.currentGroup = selectedGroup
            }
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }

    transition {
        name = "Start edit group name from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.EDIT_GROUP
        }

        action {
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.GROUP_EDIT_ENTER_NAME
        }
    }

    transition {
        name = "Start delete group from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.DELETE_GROUP
        }

        action {
            val groupId = context.currentGroup?.id
            if (groupId != null) {
                val categories = categoryService.getCategoriesByGroup(groupId)
                context.categoriesCountToDelete = categories.size
            } else {
                context.categoriesCountToDelete = 0
            }
        }

        then {
            to = MoneyManagerState.GROUP_DELETE_CONFIRM
        }
    }

    transition {
        name = "Show invite link from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.INVITE_TO_GROUP
        }

        action {
            context.isQuickGroupCreation = false
            context.inviteFromActions = true
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    transition {
        name = "View group members from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.GROUP_MEMBERS
        }

        action {
            val group = context.currentGroup
            if (group != null) {
                // Fetch UserInfo for all member IDs
                val membersList = group.memberTelegramUserIds.mapNotNull { memberId ->
                    userInfoService.getUserInfoByTelegramId(memberId)
                }
                context.groupMembersList = membersList
            }
        }

        then {
            to = MoneyManagerState.GROUP_MEMBERS
        }
    }

    transition {
        name = "Switch active group from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.SWITCH_GROUP
        }

        action {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@action

            groupService.switchActiveGroup(userId, groupId)
            context.userInfo = context.userInfo?.copy(activeGroupId = groupId)
            context.userGroups = groupService.getUserGroups(userId)
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }

    transition {
        name = "Back to group list from actions"

        condition {
            from = MoneyManagerState.GROUP_ACTIONS
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }
}