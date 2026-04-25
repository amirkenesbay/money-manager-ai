package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeEnterAmountReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_ENTER_AMOUNT

        message {
            val lang = context.userInfo?.language
            val category = context.selectedCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val categoryName = category?.name ?: ""
            val categoryLine = localizationService
                .t("finance.management.detail.category", lang, icon, categoryName)
                .trimStart('\n')
            val prompt = localizationService.t("finance.amount.prompt", lang)
            val errorText = if (context.amountInputError) {
                localizationService.t("finance.amount.error", lang)
            } else ""

            text = """
                |$categoryLine
                |
                |$prompt$errorText
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeEnterCommentReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_ENTER_COMMENT

        message {
            val lang = context.userInfo?.language
            val category = context.selectedCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val categoryName = category?.name ?: ""
            val amount = context.financeAmount ?: 0.0
            val categoryLine = localizationService
                .t("finance.management.detail.category", lang, icon, categoryName)
                .trimStart('\n')
            val amountLine = localizationService
                .t("finance.management.detail.amount", lang, amount)
                .trimStart('\n')
            val prompt = localizationService.t("finance.comment.prompt", lang)

            text = """
                |$categoryLine
                |$amountLine
                |
                |$prompt
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.comment.button.skip", lang)
                        type = MoneyManagerButtonType.SKIP_COMMENT
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_AMOUNT
                    }
                }
            }
        }
    }
}
