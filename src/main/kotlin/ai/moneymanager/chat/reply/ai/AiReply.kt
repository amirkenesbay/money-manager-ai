package ai.moneymanager.chat.reply.ai

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiModeReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_MODE

        message {
            newMessage = false
            val lang = context.userInfo?.language

            text = localizationService.t("ai.mode.intro", lang)

            keyboard {
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_CONFIRM

        message {
            newMessage = true
            val lang = context.userInfo?.language

            val description = context.pendingAiAction
                ?.describe(localizationService, lang)
                ?: localizationService.t("ai.confirm.fallback", lang)

            text = localizationService.t("ai.confirm.title", lang, description)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("ai.confirm.button.yes", lang)
                        type = MoneyManagerButtonType.CONFIRM_AI_ACTION
                    }
                    button {
                        text = localizationService.t("common.cancel", lang)
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiResultReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_RESULT

        message {
            newMessage = false
            val lang = context.userInfo?.language

            val body = context.aiResultMessage ?: localizationService.t("ai.result.empty", lang)
            val suffix = localizationService.t("ai.result.suffix", lang)
            text = body + suffix

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("common.back_to_menu", lang)
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}
