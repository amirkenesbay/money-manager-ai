package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeAmountTransitions() {
    enterAmountTransitions()
    enterCommentTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.enterAmountTransitions() {
    transition {
        name = "Enter valid amount"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_AMOUNT
            eventType = EventType.TEXT

            guard {
                val input = update.message?.text?.replace(",", ".")
                val amount = input?.toDoubleOrNull()
                amount != null && amount > 0
            }
        }

        action {
            val input = update.message?.text?.replace(",", ".") ?: return@action
            context.financeAmount = input.toDouble()
            context.amountInputError = false
        }

        then {
            to = MoneyManagerState.FINANCE_ENTER_COMMENT
        }
    }

    transition {
        name = "Enter invalid amount"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_AMOUNT
            eventType = EventType.TEXT
        }

        action {
            context.amountInputError = true
        }

        then {
            to = MoneyManagerState.FINANCE_ENTER_AMOUNT
        }
    }

    transition {
        name = "Back to category selection from amount"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_AMOUNT
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        action {
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_CATEGORY
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.enterCommentTransitions() {
    transition {
        name = "Enter comment"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_COMMENT
            eventType = EventType.TEXT
        }

        action {
            context.financeComment = update.message?.text
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_DATE
        }
    }

    transition {
        name = "Skip comment"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_COMMENT
            button = MoneyManagerButtonType.SKIP_COMMENT
        }

        action {
            context.financeComment = null
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_DATE
        }
    }

    transition {
        name = "Back to amount from comment"

        condition {
            from = MoneyManagerState.FINANCE_ENTER_COMMENT
            button = MoneyManagerButtonType.BACK_TO_AMOUNT
        }

        then {
            to = MoneyManagerState.FINANCE_ENTER_AMOUNT
        }
    }
}
