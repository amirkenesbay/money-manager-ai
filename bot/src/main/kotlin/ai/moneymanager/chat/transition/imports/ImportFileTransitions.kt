package ai.moneymanager.chat.transition.imports

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.ImportNotifier
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.telegram.isImportFile
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.model.EventType

private const val IMPORT_FILE_DIALOG_NAME = "Import File Dialog"

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.importFileDialog(importNotifier: ImportNotifier, gateway: MiniAppGateway) {
    dialog {
        name = IMPORT_FILE_DIALOG_NAME

        transition {
            name = "Read a file exported from another app"
            startDialog = true

            condition {
                eventType = EventType.DOCUMENT
                guard { isImportFile(update.message?.document) }
            }

            action {
                importNotifier.receive(chat.id, user.id, checkNotNull(update.message?.document))
            }

            then {
                to = if (gateway.enabled) MoneyManagerState.MINI_APP_GATEWAY else MoneyManagerState.MENU
                noReply = true
            }
        }
    }
}
