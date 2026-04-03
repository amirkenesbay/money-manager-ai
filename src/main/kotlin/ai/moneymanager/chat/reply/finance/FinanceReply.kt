package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeManagementReply() {
    reply {
        state = MoneyManagerState.FINANCE_MANAGEMENT

        message {
            val details = buildString {
                context.selectedCategory?.let {
                    val icon = it.icon ?: "📌"
                    append("\n$icon Категория: ${it.name}")
                }
                context.financeAmount?.let { append("\n💰 Сумма: $it") }
                context.financeComment?.let { append("\n💬 Комментарий: $it") }
                context.selectedDate?.let { append("\n📅 Дата: $it") }
            }

            text = """
                |📊 Финансы
                |
                |Управляй расходами и доходами${if (details.isNotEmpty()) "\n$details" else ""}
            """.trimMargin()

            val allFieldsFilled = context.selectedCategory != null
                && context.financeAmount != null
                && context.selectedDate != null

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расход"
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
                    }
                    button {
                        text = "📈 Доход"
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME
                    }
                }
                if (allFieldsFilled) {
                    buttonRow {
                        button {
                            text = "✅ Сохранить"
                            type = MoneyManagerButtonType.FINANCE_SAVE
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "📋 История"
                        type = MoneyManagerButtonType.FINANCE_HISTORY
                    }
                    button {
                        text = "📈 Отчёт"
                        type = MoneyManagerButtonType.FINANCE_REPORT
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}