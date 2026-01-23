package ai.moneymanager.chat.reply.nlp

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.nlpConfirmCreateGroupReply() {
    reply {
        state = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP

        message {
            newMessage = true

            text = """
                |🤖 Понял! Создаю группу.
                |
                |📝 Название: ${context.nlpGroupName}
                |
                |Создать группу с таким названием?
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Создать"
                        type = MoneyManagerButtonType.CONFIRM_NLP_ACTION
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.nlpResponseReply() {
    reply {
        state = MoneyManagerState.NLP_RESPONSE

        message {
            newMessage = true

            text = """
                |🤖 ${context.nlpResponse ?: ""}
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}