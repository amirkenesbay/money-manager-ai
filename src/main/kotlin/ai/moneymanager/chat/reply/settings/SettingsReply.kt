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
            val confirmation = settingsConfirmationBanner(context, localizationService, lang)

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
                        text = localizationService.t("settings.button.currency", lang)
                        type = MoneyManagerButtonType.OPEN_CURRENCY_PICKER
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

/** Баннер подтверждения после смены языка/валюты — обе настройки возвращают сюда, банер общий. */
private fun settingsConfirmationBanner(
    context: MoneyManagerContext,
    localizationService: LocalizationService,
    lang: String?
): String {
    val banners = buildList {
        if (context.languageJustChanged) {
            context.languageJustChanged = false
            add(localizationService.t("language.saved.confirmation", lang))
        }
        if (context.currencyJustChanged) {
            context.currencyJustChanged = false
            add(localizationService.t("currency.saved.confirmation", lang))
        }
    }
    return if (banners.isEmpty()) "" else banners.joinToString("\n") + "\n\n"
}
