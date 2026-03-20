package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

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
            context.currentGroup = createdGroup
            context.userInfo = userInfoService.getUserInfo(user)
            context.nlpGroupName = null

            log.info("✅ Group created via NLP: ${createdGroup.name}")
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
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