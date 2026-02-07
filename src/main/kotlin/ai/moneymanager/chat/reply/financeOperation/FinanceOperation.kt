package ai.moneymanager.chat.reply.financeOperation

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.selectFinanceOperationReply() {
    reply {
        state = MoneyManagerState.FINANCE_OPERATION_EXPENSE_OR_INCOME

        message {
            text = """
                Выберите категорию:
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "Получить ссылку для приглашения в группу."
                        type = MoneyManagerButtonType.INVITE_TO_GROUP

                    }
                }
                buttonRow {
                    button {
                        text = "Перейти в раздел 'Доходы'"
                        type = MoneyManagerButtonType.FINANCE_OPERATION_CREATE_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = "Перейти в раздел 'Расходы'"
                        type = MoneyManagerButtonType.FINANCE_OPERATION_CREATE_EXPENSE
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад в меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

    fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.createFinanceOperationIncomeReply() {
        reply {
            state = MoneyManagerState.FINANCE_OPERATION_CREATE_INCOME

            message {
                text = """
                Введите сумму дохода:
            """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "Перейти в раздел 'Расходы'"
                            type = MoneyManagerButtonType.FINANCE_OPERATION_CREATE_EXPENSE
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад в меню"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
    fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.createFinanceOperationExpenseReply() {
        reply {
            state = MoneyManagerState.FINANCE_OPERATION_CREATE_EXPENSE

            message {
                text = """
                Введите сумму дохода:
            """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "Перейти в раздел 'Доходы'"
                            type = MoneyManagerButtonType.FINANCE_OPERATION_CREATE_INCOME
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад в меню"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }