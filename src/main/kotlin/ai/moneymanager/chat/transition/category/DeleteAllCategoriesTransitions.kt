package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.transition.common.confirmFlow
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DeleteAllCategoriesTransitions")

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteAllCategoriesTransitions(
    categoryService: CategoryService
) {
    confirmFlow(
        flowName = "delete all categories",
        sourceState = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE,
        confirmState = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM,
        returnState = MoneyManagerState.CATEGORY_MANAGEMENT,
        triggerButton = MoneyManagerButtonType.DELETE_ALL_CATEGORIES,
        cancelState = MoneyManagerState.CATEGORY_MANAGEMENT,
        onStart = {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                context.categories = categoryService.getCategoriesByGroup(activeGroupId)
            }
        },
        onCancel = {
            context.categories = emptyList()
        },
        onConfirm = {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                val deletedCount = categoryService.deleteAllCategoriesForGroup(activeGroupId)
                context.categories = emptyList()
                context.currentCategory = null
                log.info("Deleted $deletedCount categories for group $activeGroupId")
            }
        }
    )
}
