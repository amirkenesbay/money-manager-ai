package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupDialogTransitions(
    groupService: GroupService,
    categoryService: CategoryService
) {
    openGroupManagementTransition()
    createGroupTransitions(groupService)
    viewGroupsListTransitions(groupService)
    groupActionsTransitions(groupService, categoryService)
    editGroupTransitions(groupService)
    deleteGroupTransitions(groupService)
    groupBackTransitions()
}