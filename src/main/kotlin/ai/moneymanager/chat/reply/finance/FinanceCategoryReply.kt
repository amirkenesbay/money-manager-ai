package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

private const val SELECT_CATEGORY_KEY_EXPENSE = "finance.select_category.title.expense"
private const val SELECT_CATEGORY_KEY_INCOME = "finance.select_category.title.income"
private const val NO_CATEGORIES_KEY_EXPENSE = "finance.no_categories.title.expense"
private const val NO_CATEGORIES_KEY_INCOME = "finance.no_categories.title.income"

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectCategoryReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_CATEGORY

        message {
            val lang = context.userInfo?.language
            val categories = context.categories
            val titleKey = when (context.financeOperationType) {
                CategoryType.INCOME -> SELECT_CATEGORY_KEY_INCOME
                else -> SELECT_CATEGORY_KEY_EXPENSE
            }

            text = localizationService.t(titleKey, lang)

            keyboard {
                categories.forEach { category ->
                    buttonRow {
                        button {
                            val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                            text = "$icon ${category.name}"
                            type = MoneyManagerButtonType.FINANCE_CATEGORY_ITEM
                        }
                    }
                }
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeNoCategoriesWarningReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING

        message {
            val lang = context.userInfo?.language
            val titleKey = when (context.financeOperationType) {
                CategoryType.INCOME -> NO_CATEGORIES_KEY_INCOME
                else -> NO_CATEGORIES_KEY_EXPENSE
            }
            val title = localizationService.t(titleKey, lang)
            val body = localizationService.t("finance.no_categories.body", lang)

            text = """
                |$title
                |
                |$body
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.button.create_category", lang)
                        type = MoneyManagerButtonType.CREATE_CATEGORY_FROM_FINANCE
                    }
                }
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
