package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeCategoryTransitions(
    categoryService: CategoryService
) {
    openCategorySelectionTransitions(categoryService)
    selectCategoryTransition()
    noCategoriesTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openCategorySelectionTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Open expense category selection"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
        }

        action {
            context.financeOperationType = CategoryType.EXPENSE
            context.selectedCategory = null
            loadFinanceCategories(categoryService)
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Open income category selection"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_ADD_INCOME
        }

        action {
            context.financeOperationType = CategoryType.INCOME
            context.selectedCategory = null
            loadFinanceCategories(categoryService)
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Route to category list"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            eventType = kz.rmr.chatmachinist.model.EventType.TRIGGERED
            guard { context.financeOperationType != null && context.categories.isNotEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_CATEGORY
        }
    }

    transition {
        name = "Route to no categories warning"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            eventType = kz.rmr.chatmachinist.model.EventType.TRIGGERED
            guard { context.financeOperationType != null && context.categories.isEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.selectCategoryTransition() {
    transition {
        name = "Select category for finance operation"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_CATEGORY
            button = MoneyManagerButtonType.FINANCE_CATEGORY_ITEM
        }

        action {
            val text = buttonText ?: return@action
            context.selectedCategory = context.categories.find { category ->
                val icon = category.icon ?: "📌"
                "$icon ${category.name}" == text
            }
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.FINANCE_ENTER_AMOUNT
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.noCategoriesTransitions() {
    transition {
        name = "Create category from finance warning"

        condition {
            from = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING
            button = MoneyManagerButtonType.CREATE_CATEGORY_FROM_FINANCE
        }

        action {
            context.categoryTypeInput = context.financeOperationType
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
        }
    }

    transition {
        name = "Back to finance from no categories warning"

        condition {
            from = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }

    transition {
        name = "Back to finance from category selection"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_CATEGORY
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }
}

internal fun kz.rmr.chatmachinist.model.ActionContext<MoneyManagerState, MoneyManagerContext>.loadFinanceCategories(
    categoryService: CategoryService
) {
    val activeGroupId = context.userInfo?.activeGroupId ?: return
    val type = context.financeOperationType ?: return
    context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, type)
}