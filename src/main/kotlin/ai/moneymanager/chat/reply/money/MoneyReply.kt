package ai.moneymanager.chat.reply.money

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.menuReply() {
    reply {
        state = MoneyManagerState.MENU

        message {
            val balance = context.currentBalance
            val header = if (balance != null) {
                "💰 Баланс: ${formatAmount(balance.total)}\n\n"
            } else {
                ""
            }
            text = header + """
                |Салем! 👋
                |
                |Я помогу управлять финансами. Просто напиши мне:
                |• «кофе 500» — добавить расход
                |• «зарплата 150 000» — добавить доход
                |• «создай группу семья» — совместный учёт
                |
                |Или используй кнопки 👇
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "💰 Баланс"
                        type = MoneyManagerButtonType.BALANCE
                    }
                    button {
                        text = "🔔 Уведомления"
                        type = MoneyManagerButtonType.NOTIFICATIONS
                    }
                }
                buttonRow {
                    button {
                        text = "🤖 AI"
                        type = MoneyManagerButtonType.AI_ASSISTANT
                    }
                }
                buttonRow {
                    button {
                        text = "👥 Группы"
                        type = MoneyManagerButtonType.SHARED_ACCOUNTING
                    }
                    button {
                        text = "📂 Категории"
                        type = MoneyManagerButtonType.CATEGORIES
                    }
                }
                buttonRow {
                    button {
                        text = "📊 Финансы"
                        type = MoneyManagerButtonType.FINANCE
                    }
                }
            }
        }
    }
}