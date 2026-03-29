package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.confirmFlow
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteGroupTransitions(
    groupService: GroupService,
    categoryService: CategoryService
) {
    confirmFlow(
        flowName = "delete group",
        sourceState = MoneyManagerState.GROUP_ACTIONS,
        confirmState = MoneyManagerState.GROUP_DELETE_CONFIRM,
        returnState = MoneyManagerState.GROUP_LIST,
        triggerButton = MoneyManagerButtonType.DELETE_GROUP,
        onStart = {
            val groupId = context.currentGroup?.id
            if (groupId != null) {
                context.categoriesCountToDelete = categoryService.getCategoriesByGroup(groupId).size
            } else {
                context.categoriesCountToDelete = 0
            }
        },
        onConfirm = {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@confirmFlow

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
    )
}
