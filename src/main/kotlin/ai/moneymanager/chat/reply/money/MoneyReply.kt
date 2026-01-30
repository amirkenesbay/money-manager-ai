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
                Салем, 👋🏼
                Давай научимся грамотно управлять личными финансами! 💵
                Выбери необходимые пункты ниже 👇🏼
                """.trimIndent()

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
                        text = "\uD83D\uDCB5 Расход/Доход"
                        type = MoneyManagerButtonType.EXPENSE_OR_INCOME
                    }
                }
            }
        }
    }
}