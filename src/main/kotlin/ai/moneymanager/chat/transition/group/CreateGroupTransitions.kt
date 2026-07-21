package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private const val MAX_GROUP_NAME_LENGTH = 50

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createGroupTransitions(
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
                context.groupNameInput = localizationService.t(template.nameKey, lang)
                context.isQuickGroupCreation = true
                context.currencyForPendingGroupCreation = true
            }
            then {
                to = MoneyManagerState.CURRENCY_SELECT
            }
        }
    }

    transition {
        name = "Enter group name, ask currency"
        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }
        action {
            val lang = context.userInfo?.language
            context.groupNameInput = update.message.text?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_GROUP_NAME_LENGTH)
                ?: localizationService.t("group.create.fallback_name", lang)
            context.isQuickGroupCreation = false
            context.manualTextInputActive = false
            context.customNameInputMode = false
            context.currencyForPendingGroupCreation = true
        }
        then {
            to = MoneyManagerState.CURRENCY_SELECT
        }
    }

    transition {
        name = "Group created successfully"
        condition {
            from = MoneyManagerState.CURRENCY_SELECT
            eventType = EventType.TRIGGERED
            guard { context.currencyForPendingGroupCreation && !context.groupNameDuplicateError }
        }
        action {
            context.currencyForPendingGroupCreation = false
        }
        then { to = MoneyManagerState.GROUP_INVITE_SHOW }
    }

    transition {
        name = "Group name duplicate error"
        condition {
            from = MoneyManagerState.CURRENCY_SELECT
            eventType = EventType.TRIGGERED
            guard { context.currencyForPendingGroupCreation && context.groupNameDuplicateError }
        }
        action {
            context.currencyForPendingGroupCreation = false
        }
        then { to = MoneyManagerState.GROUP_CREATE_ENTER_NAME }
    }
}
