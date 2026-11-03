package ai.moneymanager.chat.transition.digest

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.DigestNotifier
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.telegram.TelegramFileService
import ai.moneymanager.chat.telegram.mutedDigestKind
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.model.EventType

private const val DIGEST_DIALOG_NAME = "Digest Settings Dialog"

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.digestSettingsDialog(
    userInfoService: UserInfoService,
    digestNotifier: DigestNotifier,
    telegramFileService: TelegramFileService,
    gateway: MiniAppGateway,
) {
    dialog {
        name = DIGEST_DIALOG_NAME

        transition {
            name = "Mute a digest from its message"
            startDialog = true

            condition {
                eventType = EventType.COMMAND
                guard { mutedDigestKind(update.message?.text) != null }
            }

            action {
                val kind = checkNotNull(mutedDigestKind(update.message?.text))
                val language = userInfoService.setDigestMuted(user.id, kind, muted = true)?.language
                digestNotifier.confirmMuted(chat.id, kind, language)
                update.message?.messageId?.let { telegramFileService.deleteMessage(chat.id, it) }
            }

            then {
                to = if (gateway.enabled) MoneyManagerState.MINI_APP_GATEWAY else MoneyManagerState.MENU
                noReply = true
            }
        }
    }
}
