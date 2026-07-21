package ai.moneymanager.chat.reply.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.currencySelectReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CURRENCY_SELECT

        message {
            val lang = context.userInfo?.language
            val showBackButton = context.currencyReturnToSettings

            text = localizationService.t("currency.select.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("currency.button.kzt", lang)
                        type = MoneyManagerButtonType.CURRENCY_KZT
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("currency.button.usd", lang)
                        type = MoneyManagerButtonType.CURRENCY_USD
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("currency.button.rub", lang)
                        type = MoneyManagerButtonType.CURRENCY_RUB
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("currency.button.eur", lang)
                        type = MoneyManagerButtonType.CURRENCY_EUR
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("currency.button.kgs", lang)
                        type = MoneyManagerButtonType.CURRENCY_KGS
                    }
                }
                if (showBackButton) {
                    buttonRow {
                        button {
                            text = localizationService.t("common.back", lang)
                            type = MoneyManagerButtonType.BACK_TO_SETTINGS
                        }
                    }
                }
            }
        }
    }
}
