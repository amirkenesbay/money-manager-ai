package ai.moneymanager.chat.reply.settings

import ai.moneymanager.chat.reply.common.TRIBUTE_SUBSCRIBE_LINK
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.ParseMode
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.subscriptionViewReply(
    localizationService: LocalizationService,
    aiRateLimitService: AiRateLimitService
) {
    reply {
        state = MoneyManagerState.SUBSCRIPTION_VIEW

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language
            val userInfo = context.userInfo
            val hasPaidSubscription = userInfo?.hasActivePaidSubscription() == true

            val title = localizationService.t("subscription.view.title", lang)
            val body = if (hasPaidSubscription) {
                val expiresAt = userInfo?.subscriptionExpiresAt?.toLocalDate()?.format(dateFormatter).orEmpty()
                localizationService.t("subscription.view.paid", lang, expiresAt, aiRateLimitService.paidDailyLimit)
            } else {
                val freeBody = localizationService.t(
                    "subscription.view.free", lang,
                    aiRateLimitService.freeDailyLimit, aiRateLimitService.paidDailyLimit
                )
                freeBody + localizationService.t("subscription.view.upgrade_hint", lang, TRIBUTE_SUBSCRIBE_LINK)
            }

            text = "$title\n\n$body"

            keyboard {
                backButton(text = localizationService.t("common.back", lang), type = MoneyManagerButtonType.BACK_TO_SETTINGS)
            }
        }
    }
}
