package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryDialogTransitions(
    categoryService: CategoryService,
    groupService: GroupService,
    localizationService: LocalizationService
) {
    openCategoryManagementTransition(groupService)
    createCategoryTransitions(categoryService, localizationService)
    viewCategoriesListTransition(categoryService)
    categoryActionsTransitions(categoryService, localizationService)
    deleteAllCategoriesTransitions(categoryService)
    categoryBackTransitions()
}
