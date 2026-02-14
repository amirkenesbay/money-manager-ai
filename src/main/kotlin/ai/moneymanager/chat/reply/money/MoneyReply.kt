package ai.moneymanager.chat.reply.money

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.menuReply() {
    reply {
        state = MoneyManagerState.MENU

        message {
            text = """
                |Салем! 👋
                |
                |Я помогу управлять финансами. Просто напиши мне:
                |• «кофе 500» — добавить расход
                |• «зарплата 150 000» — добавить доход
                |• «создай группу семья» — создать группу
                |
                |Или используй кнопки 👇
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "👥 Совместный учет"
                        type = MoneyManagerButtonType.SHARED_ACCOUNTING
                    }
                }
                buttonRow {
                    button {
                        text = "📂 Категории"
                        type = MoneyManagerButtonType.CATEGORIES
                    }
                }
                buttonRow {
                    button {
                        text = "💡 Что спросить?"
                        type = MoneyManagerButtonType.WHAT_TO_ASK
                    }
                }
            }
        }
    }
}