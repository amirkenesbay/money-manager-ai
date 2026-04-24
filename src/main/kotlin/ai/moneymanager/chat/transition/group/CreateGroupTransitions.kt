package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.handleGroupCreated
import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private const val MAX_GROUP_NAME_LENGTH = 50

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createGroupTransitions(
    groupService: GroupService,
    localizationService: LocalizationService
) {
    simpleTransitionWithAction("Start group creation",
        MoneyManagerState.GROUP_MANAGEMENT, MoneyManagerButtonType.CREATE_GROUP, MoneyManagerState.GROUP_CREATE_ENTER_NAME
    ) { context.manualTextInputActive = true }

    simpleTransitionWithAction("Enter custom group name",
        MoneyManagerState.GROUP_CREATE_ENTER_NAME, MoneyManagerButtonType.ENTER_CUSTOM_NAME, MoneyManagerState.GROUP_CREATE_ENTER_NAME
    ) { context.customNameInputMode = true }

    simpleTransitionWithAction("Cancel group creation",
        MoneyManagerState.GROUP_CREATE_ENTER_NAME, MoneyManagerButtonType.CANCEL, MoneyManagerState.GROUP_MANAGEMENT
    ) {
        context.manualTextInputActive = false
        context.customNameInputMode = false
    }

    QuickTemplates.GROUPS.forEach { template ->
        transition {
            name = "Quick create group: ${template.nameKey}"
            condition {
                from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
                button = template.buttonType
            }
            action {
                val lang = context.userInfo?.language
                val groupName = localizationService.t(template.nameKey, lang)
                context.groupNameInput = groupName
                context.isQuickGroupCreation = true
                context.handleGroupCreated(groupService.createGroup(user.id, groupName))
            }
            then {
                to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
                noReply = true
                trigger { sameDialog = true }
            }
        }
    }

    transition {
        name = "Create group with name"
        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }
        action {
            val lang = context.userInfo?.language
            val groupName = update.message.text?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_GROUP_NAME_LENGTH)
                ?: localizationService.t("group.create.fallback_name", lang)
            context.groupNameInput = groupName
            context.isQuickGroupCreation = false
            context.manualTextInputActive = false
            context.customNameInputMode = false
            context.handleGroupCreated(groupService.createGroup(user.id, groupName))
        }
        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Group created successfully"
        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TRIGGERED
            guard { !context.groupNameDuplicateError }
        }
        then { to = MoneyManagerState.GROUP_INVITE_SHOW }
    }

    transition {
        name = "Group name duplicate error"
        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TRIGGERED
            guard { context.groupNameDuplicateError }
        }
        then { to = MoneyManagerState.GROUP_CREATE_ENTER_NAME }
    }
}
