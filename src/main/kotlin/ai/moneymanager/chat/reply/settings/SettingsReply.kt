package ai.moneymanager.chat.reply.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.settingsReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.SETTINGS

        message {
            val lang = context.userInfo?.language
            val confirmation = if (context.languageJustChanged) {
                context.languageJustChanged = false
                localizationService.t("language.saved.confirmation", lang) + "\n\n"
            } else {
                ""
            }

            text = confirmation + localizationService.t("settings.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("settings.button.language", lang)
                        type = MoneyManagerButtonType.OPEN_LANGUAGE_PICKER
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}
