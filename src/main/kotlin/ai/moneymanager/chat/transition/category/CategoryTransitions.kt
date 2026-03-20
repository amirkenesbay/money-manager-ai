package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryDialogTransitions(
    categoryService: CategoryService
) {
    openCategoryManagementTransition()
    createCategoryTransitions(categoryService)
    viewCategoriesListTransition(categoryService)
    categoryActionsTransitions(categoryService)
    deleteAllCategoriesTransitions(categoryService)
    categoryBackTransitions()
}
