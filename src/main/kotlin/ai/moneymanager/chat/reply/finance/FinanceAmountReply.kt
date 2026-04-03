package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeEnterAmountReply() {
    reply {
        state = MoneyManagerState.FINANCE_ENTER_AMOUNT

        message {
            val category = context.selectedCategory
            val icon = category?.icon ?: "📌"
            val categoryName = category?.name ?: ""
            val errorText = if (context.amountInputError) "\n\n❌ Неверный формат. Введите число, например: 1500 или 49.90" else ""

            text = """
                |$icon Категория: $categoryName
                |
                |💰 Введите сумму:${errorText}
            """.trimMargin()

            keyboard {
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeEnterCommentReply() {
    reply {
        state = MoneyManagerState.FINANCE_ENTER_COMMENT

        message {
            val category = context.selectedCategory
            val icon = category?.icon ?: "📌"
            val categoryName = category?.name ?: ""
            val amount = context.financeAmount ?: 0.0

            text = """
                |$icon Категория: $categoryName
                |💰 Сумма: $amount
                |
                |💬 Введите комментарий или нажмите "Пропустить":
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⏭ Пропустить"
                        type = MoneyManagerButtonType.SKIP_COMMENT
                    }
                }
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_AMOUNT
                    }
                }
            }
        }
    }
}
