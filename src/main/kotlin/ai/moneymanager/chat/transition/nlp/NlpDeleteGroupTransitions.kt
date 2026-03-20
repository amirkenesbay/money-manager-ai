package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.nlpDeleteGroupTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Confirm NLP delete group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
            button = MoneyManagerButtonType.CONFIRM_NLP_ACTION
        }

        action {
            val groupName = context.nlpGroupName ?: return@action
            val userId = user.id

            val group = groupService.getUserGroups(userId).find {
                matchesGroupName(it.name, groupName)
            }

            if (group == null) {
                context.nlpResponse = "❌ Группа \"$groupName\" не найдена среди ваших групп."
                context.nlpGroupName = null
                log.info("❌ Group not found for deletion: $groupName")
                return@action
            }

            if (group.ownerId != userId) {
                context.nlpResponse = "❌ Только владелец может удалить группу \"${group.name}\"."
                context.nlpGroupName = null
                log.info("❌ User $userId is not owner of group ${group.name}")
                return@action
            }

            val groupId = group.id ?: run {
                context.nlpResponse = "❌ Ошибка: группа не имеет ID."
                context.nlpGroupName = null
                log.info("❌ Group has no ID: ${group.name}")
                return@action
            }

            val deleted = groupService.deleteGroup(userId, groupId)
            context.userInfo = userInfoService.getUserInfo(user)
            context.nlpGroupName = null

            if (deleted) {
                context.nlpResponse = "✅ Группа \"${group.name}\" успешно удалена."
                log.info("✅ Group deleted via NLP: ${group.name}")
            } else {
                context.nlpResponse = "❌ Не удалось удалить группу \"${group.name}\"."
                log.info("❌ Failed to delete group: ${group.name}")
            }
        }

        then {
            to = MoneyManagerState.NLP_RESPONSE
        }
    }

    transition {
        name = "Cancel NLP delete group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
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