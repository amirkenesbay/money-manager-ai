package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand

interface AiDomainHandler {
    fun canHandle(command: BotCommand): Boolean
    fun canExecute(action: AiPendingAction): Boolean
    fun prepareAction(command: BotCommand, context: MoneyManagerContext): AiPreparationResult
    fun execute(action: AiPendingAction, context: MoneyManagerContext): String
}

sealed class AiPreparationResult {
    data class RequiresConfirmation(val action: AiPendingAction) : AiPreparationResult()
    data class ImmediateResult(val message: String) : AiPreparationResult()
    data class StateRedirect(val state: MoneyManagerState) : AiPreparationResult()
}

interface AiModeHint {
    val title: String
    val examples: List<String>
    val order: Int
}

fun MoneyManagerContext.actorId(): Long = checkNotNull(userInfo?.telegramUserId)
