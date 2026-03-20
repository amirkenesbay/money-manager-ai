package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewCategoriesListTransition(
    categoryService: CategoryService
) {
    transition {
        name = "View categories list"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.MY_CATEGORIES
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                context.categories = categoryService.getCategoriesByGroup(activeGroupId)
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }
}