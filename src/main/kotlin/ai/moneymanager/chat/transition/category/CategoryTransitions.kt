package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.SubscriptionLimitsService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryDialogTransitions(
    categoryService: CategoryService,
    groupService: GroupService,
    localizationService: LocalizationService,
    subscriptionLimitsService: SubscriptionLimitsService
) {
    openCategoryManagementTransition(groupService)
    createCategoryTransitions(categoryService, localizationService, subscriptionLimitsService)
    viewCategoriesListTransition(categoryService)
    categoryActionsTransitions(categoryService, localizationService)
    deleteAllCategoriesTransitions(categoryService)
    categoryBackTransitions()
}
