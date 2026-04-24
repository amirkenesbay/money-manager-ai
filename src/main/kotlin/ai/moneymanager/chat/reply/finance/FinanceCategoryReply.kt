package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.CategoryTypeForm
import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.categoryTypeLabel
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectCategoryReply() {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_CATEGORY

        message {
            val categories = context.categories
            val operationType = context.financeOperationType
            val typeText = categoryTypeLabel(operationType, CategoryTypeForm.GENITIVE)
            val typeEmoji = when (operationType) {
                CategoryType.EXPENSE -> "📉"
                CategoryType.INCOME -> "📈"
                else -> "📋"
            }

            text = """
                |$typeEmoji Выберите категорию $typeText
            """.trimMargin()

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
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeNoCategoriesWarningReply() {
    reply {
        state = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING

        message {
            val operationType = context.financeOperationType
            val typeText = categoryTypeLabel(operationType, CategoryTypeForm.GENITIVE_PLURAL)
            val typeEmoji = when (operationType) {
                CategoryType.EXPENSE -> "📉"
                CategoryType.INCOME -> "📈"
                else -> "📋"
            }

            text = """
                |$typeEmoji Нет категорий $typeText
                |
                |Чтобы записать операцию, сначала создайте хотя бы одну категорию.
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать категорию"
                        type = MoneyManagerButtonType.CREATE_CATEGORY_FROM_FINANCE
                    }
                }
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}