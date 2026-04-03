package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeDialogTransitions(
    categoryService: CategoryService
) {
    openFinanceManagementTransition()
    financeCategoryTransitions(categoryService)
    financeAmountTransitions()
    financeDateTransitions()
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