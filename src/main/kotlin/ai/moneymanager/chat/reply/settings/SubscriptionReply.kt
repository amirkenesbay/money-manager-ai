package ai.moneymanager.chat.reply.settings

import ai.moneymanager.chat.reply.common.TRIBUTE_SUBSCRIBE_LINK
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.SubscriptionLimitsService
import kz.rmr.chatmachinist.api.reply.ParseMode
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.subscriptionViewReply(
    localizationService: LocalizationService,
    subscriptionLimitsService: SubscriptionLimitsService
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
                localizationService.t("subscription.view.paid", lang, expiresAt, subscriptionLimitsService.maxAiRequestsPerDay(hasPaidSubscription = true))
            } else {
                val freeBody = localizationService.t(
                    "subscription.view.free", lang,
                    subscriptionLimitsService.maxAiRequestsPerDay(hasPaidSubscription = false), subscriptionLimitsService.maxAiRequestsPerDay(hasPaidSubscription = true)
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
