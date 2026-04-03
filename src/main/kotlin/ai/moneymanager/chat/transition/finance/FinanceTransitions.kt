package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceOperationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeDialogTransitions(
    categoryService: CategoryService,
    financeOperationService: FinanceOperationService
) {
    openFinanceManagementTransition()
    financeCategoryTransitions(categoryService)
    financeAmountTransitions()
    financeDateTransitions(financeOperationService)
    financeOperationSavedTransitions(categoryService)
    financeBackTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openFinanceManagementTransition() {
    transition {
        name = "Open finance management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeOperationSavedTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Add expense from saved screen"

        condition {
            from = MoneyManagerState.FINANCE_OPERATION_SAVED
            button = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
        }

        action {
            context.clearFinanceData()
            context.financeOperationType = CategoryType.EXPENSE
            loadFinanceCategories(categoryService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Add income from saved screen"

        condition {
            from = MoneyManagerState.FINANCE_OPERATION_SAVED
            button = MoneyManagerButtonType.FINANCE_ADD_INCOME
        }

        action {
            context.clearFinanceData()
            context.financeOperationType = CategoryType.INCOME
            loadFinanceCategories(categoryService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Route to category list from saved"

        condition {
            from = MoneyManagerState.FINANCE_OPERATION_SAVED
            eventType = kz.rmr.chatmachinist.model.EventType.TRIGGERED
            guard { context.financeOperationType != null && context.categories.isNotEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_CATEGORY
        }
    }

    transition {
        name = "Route to no categories from saved"

        condition {
            from = MoneyManagerState.FINANCE_OPERATION_SAVED
            eventType = kz.rmr.chatmachinist.model.EventType.TRIGGERED
            guard { context.financeOperationType != null && context.categories.isEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING
        }
    }

    transition {
        name = "Back to finance from saved screen"

        condition {
            from = MoneyManagerState.FINANCE_OPERATION_SAVED
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        action {
            context.clearFinanceData()
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeBackTransitions() {
    transition {
        name = "Back to menu from finance management"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}