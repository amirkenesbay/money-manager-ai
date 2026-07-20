package ai.moneymanager.chat.transition.finance

import ai.moneymanager.chat.reply.common.categoryButtonText
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.operationListButtonText
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import java.time.LocalDate

private const val OPERATION_LIST_LIMIT = 10

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.operationEditTransitions(
    financeHistoryService: FinanceHistoryService,
    financeOperationService: FinanceOperationService,
    categoryService: CategoryService
) {
    openOperationListTransitions(financeHistoryService)
    operationSelectionTransitions(categoryService)
    operationEditFieldTransitions(financeOperationService)
    operationDeleteTransitions(financeOperationService)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openOperationListTransitions(
    financeHistoryService: FinanceHistoryService
) {
    transition {
        name = "Open operation list from finance"
        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_EDIT_OPERATIONS
        }
        action {
            val groupId = context.userInfo?.activeGroupId
            context.operationsList = if (groupId != null) {
                financeHistoryService.getRecentOperations(groupId, OPERATION_LIST_LIMIT)
            } else {
                emptyList()
            }
        }
        then { to = MoneyManagerState.OPERATION_LIST }
    }

    transition {
        name = "Back to finance from operation list"
        condition {
            from = MoneyManagerState.OPERATION_LIST
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }
        then { to = MoneyManagerState.FINANCE_MANAGEMENT }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.operationSelectionTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Select operation from list"
        condition {
            from = MoneyManagerState.OPERATION_LIST
            button = MoneyManagerButtonType.OPERATION_LIST_ITEM
        }
        action {
            val text = buttonText
            val operation = context.operationsList.withIndex().firstOrNull { (index, operation) ->
                operationListButtonText(
                    index = index,
                    date = operation.operationDate.format(dateFormatter),
                    icon = operation.categoryIcon,
                    categoryName = operation.categoryName,
                    signedAmount = formatSignedAmount(operation.type, operation.amount)
                ) == text
            }?.value
            context.selectedOperation = operation
            if (operation != null) {
                context.categories = categoryService.getCategoriesByGroupAndType(operation.groupId, operation.type)
            }
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Back to operation list"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.BACK_TO_OPERATION_LIST
        }
        action { context.clearOperationEdit() }
        then { to = MoneyManagerState.OPERATION_LIST }
    }

    listOf(
        MoneyManagerState.OPERATION_EDIT_AMOUNT,
        MoneyManagerState.OPERATION_EDIT_SELECT_TYPE,
        MoneyManagerState.OPERATION_EDIT_SELECT_CATEGORY,
        MoneyManagerState.OPERATION_EDIT_DATE,
        MoneyManagerState.OPERATION_EDIT_COMMENT
    ).forEach { editState ->
        transition {
            name = "Back to operation actions from ${editState.name}"
            condition {
                from = editState
                button = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
            }
            action { context.operationEditError = false }
            then { to = MoneyManagerState.OPERATION_ACTIONS }
        }
    }

    transition {
        name = "Open edit amount"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_EDIT_AMOUNT_BTN
        }
        then { to = MoneyManagerState.OPERATION_EDIT_AMOUNT }
    }

    transition {
        name = "Open edit type"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_EDIT_TYPE_BTN
        }
        then { to = MoneyManagerState.OPERATION_EDIT_SELECT_TYPE }
    }

    transition {
        name = "Open edit category"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_EDIT_CATEGORY_BTN
        }
        then { to = MoneyManagerState.OPERATION_EDIT_SELECT_CATEGORY }
    }

    transition {
        name = "Open edit date"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_EDIT_DATE_BTN
        }
        then { to = MoneyManagerState.OPERATION_EDIT_DATE }
    }

    transition {
        name = "Open edit comment"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_EDIT_COMMENT_BTN
        }
        then { to = MoneyManagerState.OPERATION_EDIT_COMMENT }
    }

    transition {
        name = "Open delete confirm"
        condition {
            from = MoneyManagerState.OPERATION_ACTIONS
            button = MoneyManagerButtonType.OPERATION_DELETE_BTN
        }
        then { to = MoneyManagerState.OPERATION_DELETE_CONFIRM }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.operationEditFieldTransitions(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Edit operation amount"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_AMOUNT
            eventType = EventType.TEXT
            guard {
                val amount = update.message?.text?.replace(",", ".")?.toBigDecimalOrNull()
                amount != null && amount.signum() > 0
            }
        }
        action {
            val operation = context.selectedOperation ?: return@action
            val amount = update.message?.text?.replace(",", ".")?.toBigDecimalOrNull() ?: return@action
            context.selectedOperation = financeOperationService.updateAmount(operation.groupId, operation.id!!, amount)
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Edit operation amount invalid"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_AMOUNT
            eventType = EventType.TEXT
        }
        action { context.operationEditError = true }
        then { to = MoneyManagerState.OPERATION_EDIT_AMOUNT }
    }

    listOf(
        MoneyManagerButtonType.OPERATION_TYPE_EXPENSE to CategoryType.EXPENSE,
        MoneyManagerButtonType.OPERATION_TYPE_INCOME to CategoryType.INCOME
    ).forEach { (button, type) ->
        transition {
            name = "Edit operation type to ${type.name}"
            condition {
                from = MoneyManagerState.OPERATION_EDIT_SELECT_TYPE
                this.button = button
            }
            action {
                val operation = context.selectedOperation ?: return@action
                context.selectedOperation = financeOperationService.updateType(operation.groupId, operation.id!!, type)
            }
            then { to = MoneyManagerState.OPERATION_ACTIONS }
        }
    }

    transition {
        name = "Edit operation category"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_SELECT_CATEGORY
            button = MoneyManagerButtonType.OPERATION_CATEGORY_ITEM
        }
        action {
            val operation = context.selectedOperation ?: return@action
            val text = buttonText
            val category = context.categories.firstOrNull { categoryButtonText(it) == text } ?: return@action
            context.selectedOperation = financeOperationService.updateCategory(
                operation.groupId, operation.id!!, category.id!!, category.name, category.icon
            )
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Edit operation date to today"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_DATE
            button = MoneyManagerButtonType.QUICK_DATE_TODAY
        }
        action { updateOperationDate(context, financeOperationService, LocalDate.now()) }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Edit operation date to yesterday"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_DATE
            button = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
        }
        action { updateOperationDate(context, financeOperationService, LocalDate.now().minusDays(1)) }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Edit operation date manual"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_DATE
            eventType = EventType.TEXT
            guard { parseOperationDate(update.message?.text) != null }
        }
        action {
            val date = parseOperationDate(update.message?.text) ?: return@action
            updateOperationDate(context, financeOperationService, date)
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Edit operation date invalid"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_DATE
            eventType = EventType.TEXT
        }
        action { context.operationEditError = true }
        then { to = MoneyManagerState.OPERATION_EDIT_DATE }
    }

    transition {
        name = "Edit operation comment"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_COMMENT
            eventType = EventType.TEXT
        }
        action {
            val operation = context.selectedOperation ?: return@action
            context.selectedOperation = financeOperationService.updateDescription(
                operation.groupId, operation.id!!, update.message?.text
            )
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }

    transition {
        name = "Skip operation comment edit"
        condition {
            from = MoneyManagerState.OPERATION_EDIT_COMMENT
            button = MoneyManagerButtonType.SKIP_COMMENT
        }
        action {
            val operation = context.selectedOperation ?: return@action
            context.selectedOperation = financeOperationService.updateDescription(operation.groupId, operation.id!!, null)
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.operationDeleteTransitions(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Confirm delete operation"
        condition {
            from = MoneyManagerState.OPERATION_DELETE_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }
        action {
            val operation = context.selectedOperation ?: return@action
            financeOperationService.deleteOperation(operation.groupId, operation.id!!)
            context.operationsList = context.operationsList.filterNot { it.id == operation.id }
            context.clearOperationEdit()
        }
        then { to = MoneyManagerState.OPERATION_LIST }
    }

    transition {
        name = "Cancel delete operation"
        condition {
            from = MoneyManagerState.OPERATION_DELETE_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }
        then { to = MoneyManagerState.OPERATION_ACTIONS }
    }
}

private fun updateOperationDate(
    context: MoneyManagerContext,
    financeOperationService: FinanceOperationService,
    date: LocalDate
) {
    val operation = context.selectedOperation ?: return
    context.selectedOperation = financeOperationService.updateDate(operation.groupId, operation.id!!, date)
    context.operationEditError = false
}

private fun parseOperationDate(text: String?): LocalDate? {
    if (text.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(text) }.getOrNull()
}
