package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.domain.event.DebtReminderRequested
import ai.moneymanager.domain.event.SettlementRecorded
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.debt.DebtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.math.BigDecimal
import java.time.LocalDate

private const val REMIND_KEY = "debt.remind"
private const val SETTLED_KEY = "debt.settled"
private const val PAID_KEY = "debt.paid"
private const val PAID_MISSING_KEY = "debt.paid_missing"
private const val OPEN_BUTTON_KEY = "debt.button.open"
private const val PAID_BUTTON_KEY = "debt.button.paid"

@Service
class DebtNotifier(
    private val debtService: DebtService,
    private val groupService: GroupService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun onReminder(event: DebtReminderRequested) {
        val group = groupService.getGroup(event.groupId) ?: return
        val language = languageOf(event.debtorId)
        val text = t(REMIND_KEY, language, escapeHtml(nameOf(event.creditorId)), money(event.amount, group), escapeHtml(group.name))
        send(event.debtorId, text, reminderKeyboard(group, event.creditorId, language))
    }

    @EventListener
    fun onSettlement(event: SettlementRecorded) {
        val group = groupService.getGroup(event.groupId) ?: return
        val settlement = event.settlement
        val recipientId = if (event.actorId == settlement.fromId) settlement.toId else settlement.fromId
        val names = userInfoService.displayNames(setOf(settlement.fromId, settlement.toId))
        val text = t(SETTLED_KEY, languageOf(recipientId), escapeHtml(group.name), escapeHtml(names.getValue(settlement.fromId)), escapeHtml(names.getValue(settlement.toId)), money(settlement.amount, group))
        send(recipientId, text, null)
    }

    fun markPaid(chatId: Long, debtorId: Long, request: DebtPaidRequest, today: LocalDate, language: String?) {
        val group = groupService.getGroup(request.groupId)?.takeIf { debtorId in it.memberIds }
        val settlement = group?.let { debtService.settleTransfer(it, debtorId, request.creditorId, today) }
        val text = if (group != null && settlement != null) {
            t(PAID_KEY, language, money(settlement.amount, group), escapeHtml(nameOf(request.creditorId)))
        } else {
            t(PAID_MISSING_KEY, language)
        }
        send(chatId, text, null)
    }

    private fun reminderKeyboard(group: MoneyGroup, creditorId: Long, language: String?): InlineKeyboardMarkup {
        val paid = InlineKeyboardButton(t(PAID_BUTTON_KEY, language)).apply { url = startLink(botUsername, debtPaidParameter(checkNotNull(group.id), creditorId)) }
        val open = if (miniAppGateway.enabled) miniAppGateway.button(OPEN_BUTTON_KEY, language, MiniAppScreen.DEBTS) else null
        return InlineKeyboardMarkup(listOfNotNull(open?.let { listOf(it) }, listOf(paid)))
    }

    private fun send(chatId: Long, text: String, keyboard: InlineKeyboardMarkup?) {
        val message = SendMessage(chatId.toString(), text).apply {
            parseMode = ParseMode.HTML
            replyMarkup = keyboard
        }
        runCatching { execute(message) }.onFailure { log.warn("Failed to send debt notice to {}: {}", chatId, it.message) }
    }

    private fun money(amount: BigDecimal, group: MoneyGroup): String = formatAmount(amount, group.currency)

    private fun nameOf(userId: Long): String = userInfoService.displayNames(setOf(userId)).getValue(userId)

    private fun languageOf(userId: Long): String? = userInfoService.getUserInfoByTelegramId(userId)?.language

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
