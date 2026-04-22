package ai.moneymanager.chat.reply.ai

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.transition.ai.AI_MODE_INTRO
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiModeReply() {
    reply {
        state = MoneyManagerState.AI_MODE

        message {
            newMessage = false

            text = AI_MODE_INTRO

            keyboard {
                backButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiConfirmReply() {
    reply {
        state = MoneyManagerState.AI_CONFIRM

        message {
            newMessage = true

            val description = context.pendingAiAction?.confirmDescription
                ?: "Не удалось распознать действие."

            text = """
                |🤖 Я понял так:
                |
                |$description
                |
                |Выполнить?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Подтвердить"
                        type = MoneyManagerButtonType.CONFIRM_AI_ACTION
                    }
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiResultReply() {
    reply {
        state = MoneyManagerState.AI_RESULT

        message {
            newMessage = false

            val body = context.aiResultMessage ?: "—"
            text = """
                |$body
                |
                |💬 Можете сразу задать следующий вопрос
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ В меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}
