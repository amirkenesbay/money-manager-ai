package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.aiDialogTransitions(
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    geminiService: GeminiService,
    domainHandlers: List<AiDomainHandler>
) {
    transition {
        name = "Open AI mode from menu"
        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.AI_ASSISTANT
        }
        action {
            clearAiContext(context)
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
            handleAiText(update, context, commandParserService, domainHandlers, geminiService)
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
            handleAiVoice(update, context, commandParserService, telegramFileService, domainHandlers, geminiService)
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
            handleAiText(update, context, commandParserService, domainHandlers, geminiService)
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
            handleAiVoice(update, context, commandParserService, telegramFileService, domainHandlers, geminiService)
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
        name = "Route AI to result"
        condition {
            from = MoneyManagerState.AI_MODE
            eventType = EventType.TRIGGERED
            guard { context.pendingAiAction == null && context.aiResultMessage != null }
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
            val action = context.pendingAiAction
            if (action == null) {
                context.aiResultMessage = PARSE_ERROR_MESSAGE
                return@action
            }
            val handler = domainHandlers.firstOrNull { it.canExecute(action) }
                ?: run {
                    context.aiResultMessage = UNHANDLED_COMMAND_MESSAGE
                    context.pendingAiAction = null
                    return@action
                }
            val message = handler.execute(action, context)
            context.aiResultMessage = message
            context.pendingAiAction = null
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
            clearAiContext(context)
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
            clearAiContext(context)
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
            clearAiContext(context)
        }
        then {
            to = MoneyManagerState.MENU
        }
    }
}
