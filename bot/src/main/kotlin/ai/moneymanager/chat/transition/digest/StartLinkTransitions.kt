package ai.moneymanager.chat.transition.digest

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.DebtNotifier
import ai.moneymanager.chat.telegram.DigestNotifier
import ai.moneymanager.chat.telegram.LoanNotifier
import ai.moneymanager.chat.telegram.postponedLoanId
import ai.moneymanager.chat.telegram.repaidLoanId
import ai.moneymanager.chat.telegram.debtPaidRequest
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.telegram.RecurringNotifier
import ai.moneymanager.chat.telegram.TelegramFileService
import ai.moneymanager.chat.telegram.mutedDigestKind
import ai.moneymanager.chat.telegram.recurringRecordRequest
import ai.moneymanager.chat.telegram.undoOperationId
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType
import java.time.LocalDate
import java.time.ZoneOffset

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.startLinkDialog(
    userInfoService: UserInfoService,
    digestNotifier: DigestNotifier,
    recurringNotifier: RecurringNotifier,
    debtNotifier: DebtNotifier,
    loanNotifier: LoanNotifier,
    telegramFileService: TelegramFileService,
    gateway: MiniAppGateway,
) {
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

    startLinkTransition("Mark a due loan as paid back", { repaidLoanId(it) != null }, telegramFileService, gateway) {
        loanNotifier.markRepaid(chat.id, user.id, checkNotNull(repaidLoanId(update.message?.text)), languageOf(userInfoService))
    }

    startLinkTransition("Move a due loan a week ahead", { postponedLoanId(it) != null }, telegramFileService, gateway) {
        loanNotifier.postpone(chat.id, user.id, checkNotNull(postponedLoanId(update.message?.text)), languageOf(userInfoService))
    }

    startLinkTransition("Mark a reminded debt as paid back", { debtPaidRequest(it) != null }, telegramFileService, gateway) {
        val userInfo = userInfoService.getUserInfoByTelegramId(user.id)
        val today = LocalDate.now(userInfo?.zone ?: ZoneOffset.UTC)
        debtNotifier.markPaid(chat.id, user.id, checkNotNull(debtPaidRequest(update.message?.text)), today, userInfo?.language)
    }
}

private fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.startLinkTransition(
    transitionName: String,
    matches: (String?) -> Boolean,
    telegramFileService: TelegramFileService,
    gateway: MiniAppGateway,
    handle: ActionContext<MoneyManagerState, MoneyManagerContext>.() -> Unit,
) {
    dialog {
        name = transitionName

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
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.languageOf(userInfoService: UserInfoService): String? =
    userInfoService.getUserInfoByTelegramId(user.id)?.language
