package ai.moneymanager.chat.reply.settings

import ai.moneymanager.chat.reply.common.TRIBUTE_SUBSCRIBE_LINK
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.ParseMode
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.proInfoReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.PRO_INFO

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language

            val title = localizationService.t("pro.info.title", lang)
            val body = localizationService.t("pro.info.body", lang)
            val note = localizationService.t("pro.info.activation_note", lang)

            text = "$title\n\n$body\n\n$note"

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("pro.info.button.subscribe", lang)
                        type = MoneyManagerButtonType.SUBSCRIBE_PRO_LINK
                        link = TRIBUTE_SUBSCRIBE_LINK
                    }
                }
            }
        }
    }
}
