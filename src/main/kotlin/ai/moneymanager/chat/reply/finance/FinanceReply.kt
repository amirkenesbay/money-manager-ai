package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeManagementReply() {
    reply {
        state = MoneyManagerState.FINANCE_MANAGEMENT

        message {
            val details = context.buildFinanceDetails()

            text = """
                |📊 Финансы
                |
                |Управляй расходами и доходами${if (details.isNotEmpty()) "\n$details" else ""}
            """.trimMargin()

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

private fun MoneyManagerContext.buildFinanceDetails(): String = buildString {
    selectedCategory?.let {
        val icon = it.icon ?: "📌"
        append("\n$icon Категория: ${it.name}")
    }
    financeAmount?.let { append("\n💰 Сумма: $it") }
    financeComment?.let { append("\n💬 Комментарий: $it") }
    selectedDate?.let { append("\n📅 Дата: $it") }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeOperationSavedReply() {
    reply {
        state = MoneyManagerState.FINANCE_OPERATION_SAVED

        message {
            val typeLabel = when (context.financeOperationType) {
                CategoryType.INCOME -> "Доход"
                else -> "Расход"
            }

            text = """
                |✅ $typeLabel добавлен!
                |${context.buildFinanceDetails()}
            """.trimMargin()

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
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}
