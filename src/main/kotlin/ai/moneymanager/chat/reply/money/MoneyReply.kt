package ai.moneymanager.chat.reply.money

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.menuReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.MENU

        message {
            val lang = context.userInfo?.language
            val balance = context.currentBalance
            val header = if (balance != null) {
                localizationService.t("menu.balance_prefix", lang, formatAmount(balance.total)) + "\n\n"
            } else {
                ""
            }

            val body = localizationService.t("menu.body", lang)
            val balanceText = localizationService.t("menu.button.balance", lang)
            val notificationsText = localizationService.t("menu.button.notifications", lang)
            val aiText = localizationService.t("menu.button.ai", lang)
            val groupsText = localizationService.t("menu.button.groups", lang)
            val categoriesText = localizationService.t("menu.button.categories", lang)
            val financeText = localizationService.t("menu.button.finance", lang)
            val settingsText = localizationService.t("menu.button.settings", lang)

            text = header + body

            keyboard {
                buttonRow {
                    button {
                        text = balanceText
                        type = MoneyManagerButtonType.BALANCE
                    }
                    button {
                        text = notificationsText
                        type = MoneyManagerButtonType.NOTIFICATIONS
                    }
                }
                buttonRow {
                    button {
                        text = aiText
                        type = MoneyManagerButtonType.AI_ASSISTANT
                    }
                }
                buttonRow {
                    button {
                        text = groupsText
                        type = MoneyManagerButtonType.SHARED_ACCOUNTING
                    }
                    button {
                        text = categoriesText
                        type = MoneyManagerButtonType.CATEGORIES
                    }
                }
                buttonRow {
                    button {
                        text = financeText
                        type = MoneyManagerButtonType.FINANCE
                    }
                }
                buttonRow {
                    button {
                        text = settingsText
                        type = MoneyManagerButtonType.SETTINGS
                    }
                }
            }
        }
    }
}
