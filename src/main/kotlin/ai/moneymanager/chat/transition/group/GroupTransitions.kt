package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupDialogTransitions(
    groupService: GroupService,
    categoryService: CategoryService,
    userInfoService: UserInfoService
) {
    openGroupManagementTransition()
    createGroupTransitions(groupService)
    viewGroupsListTransitions(groupService)
    groupActionsTransitions(groupService, userInfoService)
    editGroupTransitions(groupService)
    deleteGroupTransitions(groupService, categoryService)
    groupBackTransitions()
}
