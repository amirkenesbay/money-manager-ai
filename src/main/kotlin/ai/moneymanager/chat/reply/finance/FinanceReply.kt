package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeManagementReply() {
    reply {
        state = MoneyManagerState.FINANCE_MANAGEMENT

        message {
            text = """
                |📊 Финансы
                |
                |Управляй расходами и доходами
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расход"
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE // TODO: добавление расхода
                    }
                    button {
                        text = "📈 Доход"
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME // TODO: добавление дохода
                    }
                }
                buttonRow {
                    button {
                        text = "📋 История"
                        type = MoneyManagerButtonType.FINANCE_HISTORY // TODO: история операций
                    }
                    button {
                        text = "📈 Отчёт"
                        type = MoneyManagerButtonType.FINANCE_REPORT // TODO: отчёт по операциям
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