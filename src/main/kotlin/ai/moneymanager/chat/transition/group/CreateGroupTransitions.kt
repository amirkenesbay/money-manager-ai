package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private const val MAX_GROUP_NAME_LENGTH = 50

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.quickCreateGroupTransition(
    groupService: GroupService,
    transitionName: String,
    buttonType: MoneyManagerButtonType,
    groupName: String
) {
    transition {
        name = transitionName

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = buttonType
        }

        action {
            context.groupNameInput = groupName
            context.isQuickGroupCreation = true

            val createdGroup = groupService.createGroup(user.id, groupName)
            if (createdGroup != null) {
                context.currentGroup = createdGroup
                context.userInfo = context.userInfo?.copy(
                    activeGroupId = createdGroup.id,
                    groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
                )
                context.groupNameDuplicateError = false
            } else {
                context.groupNameDuplicateError = true
            }
        }

        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createGroupTransitions(
    groupService: GroupService
) {
    transition {
        name = "Start group creation"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_GROUP
        }

        action {
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
        }
    }

    transition {
        name = "Cancel group creation"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    quickCreateGroupTransition(groupService, "Quick create group: Family", MoneyManagerButtonType.QUICK_GROUP_FAMILY, "Семья")
    quickCreateGroupTransition(groupService, "Quick create group: With Friend", MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND, "С другом")
    quickCreateGroupTransition(groupService, "Quick create group: Work", MoneyManagerButtonType.QUICK_GROUP_WORK, "Работа")
    quickCreateGroupTransition(groupService, "Quick create group: Trip", MoneyManagerButtonType.QUICK_GROUP_TRIP, "Поездка")

    transition {
        name = "Create group with name"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val groupName = update.message.text?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_GROUP_NAME_LENGTH)
                ?: "Моя группа"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = false

            val createdGroup = groupService.createGroup(user.id, groupName)
            if (createdGroup != null) {
                context.manualTextInputActive = false
                context.currentGroup = createdGroup
                context.userInfo = context.userInfo?.copy(
                    activeGroupId = createdGroup.id,
                    groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
                )
                context.groupNameDuplicateError = false
            } else {
                context.groupNameDuplicateError = true
            }
        }

        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Triggered: group created successfully
    transition {
        name = "Group created successfully"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TRIGGERED
            guard { !context.groupNameDuplicateError }
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Triggered: duplicate group name
    transition {
        name = "Group name duplicate error"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TRIGGERED
            guard { context.groupNameDuplicateError }
        }

        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
        }
    }
}
