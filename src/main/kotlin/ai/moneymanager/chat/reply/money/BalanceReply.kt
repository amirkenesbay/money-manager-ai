package ai.moneymanager.chat.reply.money

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceOnboardingPromptReply() {
    reply {
        state = MoneyManagerState.BALANCE_ONBOARDING_PROMPT

        message {
            text = """
                |👋 Добро пожаловать!
                |
                |Чтобы я точно считал твои деньги, расскажи, сколько у тебя сейчас на счетах.
                |
                |Это нужно один раз — дальше я буду обновлять баланс автоматически при каждом расходе и доходе.
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "💵 Указать текущий баланс"
                        type = MoneyManagerButtonType.BALANCE_SET_AMOUNT
                    }
                }
                buttonRow {
                    button {
                        text = "✨ Начать с нуля"
                        type = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceOnboardingEnterAmountReply() {
    reply {
        state = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT

        message {
            val errorPrefix = if (context.balanceAmountInputError) {
                "⚠️ Не получилось распознать сумму. Введите положительное число, например: 50000\n\n"
            } else {
                ""
            }
            text = errorPrefix + """
                |💵 Введите текущий баланс
                |
                |Например: 50000 или 150 000
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✨ Начать с нуля"
                        type = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.balanceViewReply() {
    reply {
        state = MoneyManagerState.BALANCE_VIEW

        message {
            val balance = context.currentBalance
            text = if (balance != null) {
                """
                    |💰 Баланс
                    |
                    |Начальный: ${formatAmount(balance.initial)}
                    |📈 Доходы: ${formatAmount(balance.income)}
                    |📉 Расходы: ${formatAmount(balance.expense)}
                    |
                    |Итого: ${formatAmount(balance.total)}
                """.trimMargin()
            } else {
                """
                    |💰 Баланс
                    |
                    |Пока нет данных о балансе.
                """.trimMargin()
            }

            keyboard {
                backButton()
            }
        }
    }
}

