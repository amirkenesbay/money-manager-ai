package ai.moneymanager.chat.reply.nlp

import ai.moneymanager.chat.reply.common.backButton
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.nlpConfirmDeleteGroupReply() {
    reply {
        state = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP

        message {
            newMessage = true

            text = """
                |⚠️ Удаление группы
                |
                |📝 Группа: ${context.nlpGroupName}
                |
                |Вы уверены, что хотите удалить эту группу?
                |Все данные группы будут удалены безвозвратно.
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "🗑️ Удалить"
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
            newMessage = context.nlpNewMessage

            text = """
                |🤖 ${context.nlpResponse ?: ""}
            """.trimMargin()

            keyboard {
                backButton()
            }
        }
    }
}