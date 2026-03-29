package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.transition.common.loadCategoriesByType
import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewCategoriesListTransition(
    categoryService: CategoryService
) {
    simpleTransition("View categories - select type",
        MoneyManagerState.CATEGORY_MANAGEMENT, MoneyManagerButtonType.MY_CATEGORIES, MoneyManagerState.CATEGORY_LIST_SELECT_TYPE)

    transition {
        name = "View expense categories"
        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
        }
        action { context.loadCategoriesByType(CategoryType.EXPENSE, categoryService) }
        then { to = MoneyManagerState.CATEGORY_LIST }
    }

    transition {
        name = "View income categories"
        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
        }
        action { context.loadCategoriesByType(CategoryType.INCOME, categoryService) }
        then { to = MoneyManagerState.CATEGORY_LIST }
    }
}
