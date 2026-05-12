package ai.moneymanager.chat.reply.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.languageSelectReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.LANGUAGE_SELECT

        message {
            val lang = context.userInfo?.language
            val showBackButton = context.languageReturnToSettings
            val ruText = localizationService.t("language.button.ru", lang)
            val enText = localizationService.t("language.button.en", lang)
            val kkText = localizationService.t("language.button.kk", lang)
            val backText = localizationService.t("common.back", lang)

            text = localizationService.t("language.select.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = ruText
                        type = MoneyManagerButtonType.LANGUAGE_RU
                    }
                }
                buttonRow {
                    button {
                        text = enText
                        type = MoneyManagerButtonType.LANGUAGE_EN
                    }
                }
                buttonRow {
                    button {
                        text = kkText
                        type = MoneyManagerButtonType.LANGUAGE_KK
                    }
                }
                if (showBackButton) {
                    buttonRow {
                        button {
                            text = backText
                            type = MoneyManagerButtonType.BACK_TO_SETTINGS
                        }
                    }
                }
            }
        }
    }
}
