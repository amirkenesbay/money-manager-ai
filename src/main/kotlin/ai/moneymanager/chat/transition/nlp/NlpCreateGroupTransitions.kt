package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.nlpCreateGroupTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Confirm NLP create group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            button = MoneyManagerButtonType.CONFIRM_NLP_ACTION
        }

        action {
            val groupName = context.nlpGroupName ?: return@action

            val createdGroup = groupService.createGroup(user.id, groupName)
            if (createdGroup != null) {
                context.currentGroup = createdGroup
                context.userInfo = userInfoService.getUserInfo(user)
                context.groupNameDuplicateError = false
                log.info("✅ Group created via NLP: ${createdGroup.name}")
            } else {
                context.groupNameDuplicateError = true
                context.groupNameInput = groupName
                log.info("❌ Duplicate group name via NLP: $groupName")
            }
            context.nlpGroupName = null
        }

        then {
            to = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Triggered: NLP group created successfully
    transition {
        name = "NLP group created successfully"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            eventType = EventType.TRIGGERED
            guard { !context.groupNameDuplicateError }
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Triggered: NLP duplicate group name
    transition {
        name = "NLP group name duplicate error"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            eventType = EventType.TRIGGERED
            guard { context.groupNameDuplicateError }
        }

        action {
            context.nlpResponse = "❌ Группа с названием «${context.groupNameInput}» уже существует"
            context.groupNameDuplicateError = false
        }

        then {
            to = MoneyManagerState.NLP_RESPONSE
        }
    }

    transition {
        name = "Cancel NLP create group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.nlpGroupName = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}