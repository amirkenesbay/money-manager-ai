package ai.moneymanager.chat.transition.ai

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.chat.reply.common.expandableBlockquote
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.aiDialogTransitions(
    actionExecutor: AiActionExecutor,
    requestHandler: AiRequestHandler,
    localizationService: LocalizationService
) {
    transition {
        name = "Open AI mode from menu"
        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.AI_ASSISTANT
        }
        action {
            actionExecutor.clear(context)
        }
        then {
            to = MoneyManagerState.AI_MODE
        }
    }

    transition {
        name = "Process text in AI mode"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TEXT
            guard {
                context.isActive &&
                    (update.message?.text?.let { !it.startsWith("/") } ?: false)
            }
        }
        action {
            requestHandler.processText(this)
        }
        then {
            to = MoneyManagerState.AI_MODE
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Process voice in AI mode"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.VOICE
        }
        action {
            requestHandler.processVoice(this)
        }
        then {
            to = MoneyManagerState.AI_MODE
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Process text in AI result"
        condition {
            from = MoneyManagerState.AI_RESULT
            eventType = EventType.TEXT
            guard {
                context.isActive &&
                    (update.message?.text?.let { !it.startsWith("/") } ?: false)
            }
        }
        action {
            requestHandler.processText(this)
        }
        then {
            to = MoneyManagerState.AI_MODE
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Process voice in AI result"
        condition {
            from = MoneyManagerState.AI_RESULT
            eventType = EventType.VOICE
        }
        action {
            requestHandler.processVoice(this)
        }
        then {
            to = MoneyManagerState.AI_MODE
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Route AI to confirm"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard { context.pendingAiAction != null }
        }
        then {
            to = MoneyManagerState.AI_CONFIRM
        }
    }

    transition {
        name = "Show AI hints"
        condition {
            from = MoneyManagerState.AI_MODE
            button = MoneyManagerButtonType.WHAT_TO_ASK
        }
        action {
            actionExecutor.clear(context)
            context.aiResultMessage = expandableBlockquote(localizationService.t("ai.hints", context.userInfo?.language))
        }
        then {
            to = MoneyManagerState.AI_RESULT
        }
    }

    transition {
        name = "Route AI to batch confirm"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard { context.pendingAiActions.isNotEmpty() }
        }
        then {
            to = MoneyManagerState.AI_CONFIRM_BATCH
        }
    }

    transition {
        name = "Route AI to result"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard {
                context.pendingAiAction == null &&
                    context.pendingAiActions.isEmpty() &&
                    context.aiResultMessage != null
            }
        }
        then {
            to = MoneyManagerState.AI_RESULT
        }
    }

    transition {
        name = "Route AI to category list select type"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard { context.aiRedirectState == MoneyManagerState.CATEGORY_LIST_SELECT_TYPE }
        }
        action { context.aiRedirectState = null }
        then {
            to = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
        }
    }

    transition {
        name = "Route AI to category list"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard { context.aiRedirectState == MoneyManagerState.CATEGORY_LIST }
        }
        action { context.aiRedirectState = null }
        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }

    transition {
        name = "Confirm AI action"
        condition {
            from = MoneyManagerState.AI_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_AI_ACTION
        }
        action {
            actionExecutor.execute(context)
        }
        then {
            to = MoneyManagerState.AI_RESULT
        }
    }

    transition {
        name = "Open AI category picker"
        condition {
            from = MoneyManagerState.AI_CONFIRM
            button = MoneyManagerButtonType.AI_PICK_DIFFERENT_CATEGORY
        }
        then {
            to = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY
        }
    }

    transition {
        name = "AI picker show all"
        condition {
            from = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY
            button = MoneyManagerButtonType.AI_PICK_SHOW_ALL
        }
        then {
            to = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY_ALL
        }
    }

    transition {
        name = "AI picker back from top to confirm"
        condition {
            from = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY
            button = MoneyManagerButtonType.AI_PICK_BACK_TO_CONFIRM
        }
        then {
            to = MoneyManagerState.AI_CONFIRM
        }
    }

    transition {
        name = "AI picker back from all to confirm"
        condition {
            from = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY_ALL
            button = MoneyManagerButtonType.AI_PICK_BACK_TO_CONFIRM
        }
        then {
            to = MoneyManagerState.AI_CONFIRM
        }
    }

    aiPickerCategoryClickTransition(
        transitionName = "AI picker top: select category",
        fromState = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY,
        actionExecutor = actionExecutor,
        localizationService = localizationService
    )

    aiPickerCategoryClickTransition(
        transitionName = "AI picker all: select category",
        fromState = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY_ALL,
        actionExecutor = actionExecutor,
        localizationService = localizationService
    )

    transition {
        name = "Confirm AI batch"
        condition {
            from = MoneyManagerState.AI_CONFIRM_BATCH
            button = MoneyManagerButtonType.CONFIRM_AI_ACTION
        }
        action {
            actionExecutor.executeBatch(context)
        }
        then {
            to = MoneyManagerState.AI_RESULT
        }
    }

    transition {
        name = "Cancel AI action"
        condition {
            from = MoneyManagerState.AI_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }
        action {
            actionExecutor.clear(context)
        }
        then {
            to = MoneyManagerState.AI_MODE
        }
    }

    transition {
        name = "Cancel AI batch"
        condition {
            from = MoneyManagerState.AI_CONFIRM_BATCH
            button = MoneyManagerButtonType.CANCEL
        }
        action {
            actionExecutor.clear(context)
        }
        then {
            to = MoneyManagerState.AI_MODE
        }
    }

    transition {
        name = "Back to menu from AI mode"
        condition {
            from = MoneyManagerState.AI_MODE
            button = MoneyManagerButtonType.BACK_TO_MENU
        }
        action {
            actionExecutor.clear(context)
        }
        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Back to menu from AI result"
        condition {
            from = MoneyManagerState.AI_RESULT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }
        action {
            actionExecutor.clear(context)
        }
        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.aiPickerCategoryClickTransition(
    transitionName: String,
    fromState: MoneyManagerState,
    actionExecutor: AiActionExecutor,
    localizationService: LocalizationService
) {
    transition {
        name = transitionName
        condition {
            from = fromState
            button = MoneyManagerButtonType.AI_PICK_CATEGORY_ITEM
        }
        action {
            val lang = context.userInfo?.language
            val text = buttonText
            val currentAction = context.pendingAiAction as? AiPendingAction.TransactionAction
            if (text == null || currentAction == null) {
                context.aiResultMessage = localizationService.t("ai.error.parse", lang)
                context.pendingAiAction = null
                return@action
            }
            val candidates = context.aiCategoriesCache.orEmpty().filter { it.type == currentAction.type }
            val chosen = findCategoryByButtonText(text, candidates)
            if (chosen == null) {
                context.aiResultMessage = localizationService.t("ai.error.unhandled", lang)
                context.pendingAiAction = null
                return@action
            }
            context.pendingAiAction = currentAction.withCategory(chosen)
            actionExecutor.execute(context)
        }
        then {
            to = MoneyManagerState.AI_RESULT
        }
    }
}
