package ai.moneymanager.chat.reply.money

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.formatBalanceBreakdown
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.ParseMode
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceOnboardingPromptReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.BALANCE_ONBOARDING_PROMPT

        message {
            val lang = context.userInfo?.language
            val title = localizationService.t("balance.onboarding.title", lang)
            val body = localizationService.t("balance.onboarding.body", lang)

            text = """
                |$title
                |
                |$body
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("balance.onboarding.button.set_amount", lang)
                        type = MoneyManagerButtonType.BALANCE_SET_AMOUNT
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("balance.onboarding.button.start_zero", lang)
                        type = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceOnboardingEnterAmountReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT

        message {
            val lang = context.userInfo?.language
            val errorPrefix = if (context.balanceAmountInputError) {
                localizationService.t("balance.onboarding.amount.error", lang)
            } else ""
            val prompt = localizationService.t("balance.onboarding.amount.title", lang)

            text = errorPrefix + prompt

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("balance.onboarding.button.start_zero", lang)
                        type = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceViewReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.BALANCE_VIEW

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language
            val balance = context.currentBalance

            text = if (balance != null) {
                formatBalanceBreakdown(balance, localizationService, lang)
            } else {
                """
                    |${localizationService.t("balance.view.title", lang)}
                    |
                    |${localizationService.t("balance.view.empty", lang)}
                """.trimMargin()
            }

            keyboard {
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}
