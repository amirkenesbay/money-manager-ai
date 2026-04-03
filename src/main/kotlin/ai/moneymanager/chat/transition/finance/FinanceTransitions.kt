package ai.moneymanager.chat.transition.finance

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
    financeDateTransitions()
    saveFinanceOperationTransition(financeOperationService)
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

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.saveFinanceOperationTransition(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Save finance operation"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_SAVE
        }

        action {
            val groupId = context.userInfo?.activeGroupId ?: return@action
            val type = context.financeOperationType ?: return@action
            val amount = context.financeAmount ?: return@action
            val category = context.selectedCategory ?: return@action
            val date = context.selectedDate ?: return@action

            financeOperationService.save(
                groupId = groupId,
                creatorId = user.id,
                type = type,
                amount = amount,
                categoryId = category.id!!,
                categoryName = category.name,
                categoryIcon = category.icon,
                operationDate = date,
                description = context.financeComment
            )

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