package ai.moneymanager.chat.transition.digest

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.DebtNotifier
import ai.moneymanager.chat.telegram.DigestNotifier
import ai.moneymanager.chat.telegram.debtPaidRequest
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.telegram.RecurringNotifier
import ai.moneymanager.chat.telegram.TelegramFileService
import ai.moneymanager.chat.telegram.mutedDigestKind
import ai.moneymanager.chat.telegram.recurringRecordRequest
import ai.moneymanager.chat.telegram.undoOperationId
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType
import java.time.LocalDate
import java.time.ZoneOffset

private const val START_LINK_DIALOG_NAME = "Start Link Dialog"

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.startLinkDialog(
    userInfoService: UserInfoService,
    digestNotifier: DigestNotifier,
    recurringNotifier: RecurringNotifier,
    debtNotifier: DebtNotifier,
    telegramFileService: TelegramFileService,
    gateway: MiniAppGateway,
) {
    dialog {
        name = START_LINK_DIALOG_NAME

        startLinkTransition("Mute a digest from its message", { mutedDigestKind(it) != null }, telegramFileService, gateway) {
            val kind = checkNotNull(mutedDigestKind(update.message?.text))
            val language = userInfoService.setDigestMuted(user.id, kind, muted = true)?.language
            digestNotifier.confirmMuted(chat.id, kind, language)
        }

        startLinkTransition("Undo an operation from its message", { undoOperationId(it) != null }, telegramFileService, gateway) {
            recurringNotifier.undo(chat.id, user.id, checkNotNull(undoOperationId(update.message?.text)), languageOf(userInfoService))
        }

        startLinkTransition("Record a reminded recurring payment", { recurringRecordRequest(it) != null }, telegramFileService, gateway) {
            recurringNotifier.recordNow(chat.id, user.id, checkNotNull(recurringRecordRequest(update.message?.text)), languageOf(userInfoService))
        }

        startLinkTransition("Mark a reminded debt as paid back", { debtPaidRequest(it) != null }, telegramFileService, gateway) {
            val userInfo = userInfoService.getUserInfoByTelegramId(user.id)
            val today = LocalDate.now(userInfo?.zone ?: ZoneOffset.UTC)
            debtNotifier.markPaid(chat.id, user.id, checkNotNull(debtPaidRequest(update.message?.text)), today, userInfo?.language)
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.startLinkTransition(
    transitionName: String,
    matches: (String?) -> Boolean,
    telegramFileService: TelegramFileService,
    gateway: MiniAppGateway,
    handle: ActionContext<MoneyManagerState, MoneyManagerContext>.() -> Unit,
) {
    transition {
        name = transitionName
        startDialog = true

        condition {
            eventType = EventType.COMMAND
            guard { matches(update.message?.text) }
        }

        action {
            handle()
            update.message?.messageId?.let { telegramFileService.deleteMessage(chat.id, it) }
        }

        then {
            to = if (gateway.enabled) MoneyManagerState.MINI_APP_GATEWAY else MoneyManagerState.MENU
            noReply = true
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.languageOf(userInfoService: UserInfoService): String? =
    userInfoService.getUserInfoByTelegramId(user.id)?.language
