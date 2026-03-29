package ai.moneymanager.chat.reply.common

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.KeyboardBuilder

fun KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.backButton(
    text: String = "⬅️ Назад",
    type: MoneyManagerButtonType = MoneyManagerButtonType.BACK_TO_MENU
) {
    buttonRow {
        button {
            this.text = text
            this.type = type
        }
    }
}

fun KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.cancelButton(
    text: String = "❌ Отмена"
) {
    buttonRow {
        button {
            this.text = text
            this.type = MoneyManagerButtonType.CANCEL
        }
    }
}

fun KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.confirmAndCancelButtons(
    confirmText: String = "✅ Да, удалить",
    confirmType: MoneyManagerButtonType = MoneyManagerButtonType.CONFIRM_DELETE,
    cancelText: String = "❌ Отмена"
) {
    buttonRow {
        button {
            text = confirmText
            type = confirmType
        }
    }
    cancelButton(cancelText)
}
