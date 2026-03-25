package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewCategoriesListTransition(
    categoryService: CategoryService
) {
    transition {
        name = "View categories - select type"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.MY_CATEGORIES
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
        }
    }

    transition {
        name = "View expense categories"

        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                context.categoryTypeInput = CategoryType.EXPENSE
                context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, CategoryType.EXPENSE)
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }

    transition {
        name = "View income categories"

        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                context.categoryTypeInput = CategoryType.INCOME
                context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, CategoryType.INCOME)
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }
}