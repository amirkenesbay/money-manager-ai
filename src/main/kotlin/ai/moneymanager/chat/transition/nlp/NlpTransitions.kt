package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.nlp.CommandParserService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.nlpDialogTransitions(
    commandParserService: CommandParserService,
    groupService: GroupService,
    userInfoService: UserInfoService,
    telegramFileService: TelegramFileService,
    geminiService: GeminiService
) {
    transition {
        name = "Show what to ask hints"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.WHAT_TO_ASK
        }

        action {
            context.nlpResponse = WHAT_TO_ASK_MESSAGE
            context.nlpNewMessage = false
        }

        then {
            to = MoneyManagerState.NLP_RESPONSE
        }
    }

    listOf(MoneyManagerState.MENU, MoneyManagerState.NLP_RESPONSE).forEach { state ->
        createTextInputTransition(state, commandParserService, geminiService)
        createVoiceInputTransition(state, commandParserService, telegramFileService, geminiService)
    }

    val sourceStates = listOf(MoneyManagerState.MENU, MoneyManagerState.NLP_RESPONSE)
    val targetStates = listOf(
        MoneyManagerState.NLP_CONFIRM_CREATE_GROUP,
        MoneyManagerState.NLP_CONFIRM_DELETE_GROUP,
        MoneyManagerState.NLP_RESPONSE
    )

    sourceStates.forEach { source ->
        targetStates.forEach { target ->
            createNlpRouter(source, target)
        }
    }

    transition {
        name = "Back to menu from NLP response"

        condition {
            from = MoneyManagerState.NLP_RESPONSE
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        action {
            context.nlpResponse = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    nlpCreateGroupTransitions(groupService, userInfoService)
    nlpDeleteGroupTransitions(groupService, userInfoService)
}