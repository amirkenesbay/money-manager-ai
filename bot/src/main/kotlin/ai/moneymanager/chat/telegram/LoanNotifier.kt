package ai.moneymanager.chat.telegram

import ai.moneymanager.metrics.MessageType
import ai.moneymanager.metrics.MessageMetrics
import ai.moneymanager.chat.reply.common.resolveCurrency
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.chat.reply.loan.LoanFormatter
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.loan.LoanService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val LOAN_CHECK_INTERVAL_MS = 300_000L
private const val LOOKAHEAD_DAYS = 1L
private val REMIND_TIME: LocalTime = LocalTime.of(10, 0)
private const val POSTPONED_KEY = "loan.postponed"
private const val REPAID_BUTTON_KEY = "loan.button.repaid"
private const val LATER_BUTTON_KEY = "loan.button.later"
private const val OPEN_BUTTON_KEY = "loan.button.open"

@Service
class LoanNotifier(
    private val loanService: LoanService,
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val loanFormatter: LoanFormatter,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    private val messageMetrics: MessageMetrics,
    private val clock: Clock,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = LOAN_CHECK_INTERVAL_MS)
    fun remindDueLoans() {
        loanService.dueBy(LocalDate.now(clock).plusDays(LOOKAHEAD_DAYS)).forEach { loan ->
            runCatching { remindIfDue(loan) }.onFailure { log.warn("Failed to remind about loan {}: {}", loan.id, it.message) }
        }
    }

    fun markRepaid(chatId: Long, userId: Long, loanId: ObjectId, language: String?) {
        val today = todayOf(userId)
        val text = loanService.repayAll(loanId, userId, today)
            ?.let { loanFormatter.repaid(it, resolveCurrency(groupService, it.groupId), language) }
            ?: localizationService.t("debt.paid_missing", language)
        send(chatId, text, null)
    }

    fun postpone(chatId: Long, userId: Long, loanId: ObjectId, language: String?) {
        val text = loanService.postpone(loanId, userId, todayOf(userId))
            ?.let { localizationService.t(POSTPONED_KEY, language, checkNotNull(it.dueDate).format(shortDateFormatter)) }
            ?: localizationService.t("debt.paid_missing", language)
        send(chatId, text, null)
    }

    private fun remindIfDue(loan: LoanEntity) {
        val user = userInfoService.getUserInfoByTelegramId(loan.creatorId) ?: return
        val now = LocalDateTime.now(clock.withZone(user.zone))
        val due = checkNotNull(loan.dueDate)
        if (due.isAfter(now.toLocalDate()) || now.toLocalTime().isBefore(REMIND_TIME)) return
        if (!loanService.claimDueReminder(loan)) return
        val language = user.language
        send(loan.creatorId, loanFormatter.due(loan, resolveCurrency(groupService, loan.groupId), language), keyboard(checkNotNull(loan.id), language))
    }

    private fun keyboard(loanId: ObjectId, language: String?): InlineKeyboardMarkup {
        fun link(key: String, parameter: String) = InlineKeyboardButton(localizationService.t(key, language)).apply { url = startLink(botUsername, parameter) }
        val open = if (miniAppGateway.enabled) miniAppGateway.button(OPEN_BUTTON_KEY, language, MiniAppScreen.DEBTS, source = MessageType.LOAN) else null
        return InlineKeyboardMarkup(
            listOfNotNull(
                listOf(link(REPAID_BUTTON_KEY, loanRepaidParameter(loanId)), link(LATER_BUTTON_KEY, loanLaterParameter(loanId))),
                open?.let { listOf(it) },
            )
        )
    }

    private fun todayOf(userId: Long): LocalDate {
        val zone = userInfoService.getUserInfoByTelegramId(userId)?.zone ?: clock.zone
        return LocalDate.now(clock.withZone(zone))
    }

    private fun send(chatId: Long, text: String, keyboard: InlineKeyboardMarkup?) {
        val message = SendMessage(chatId.toString(), text).apply {
            parseMode = ParseMode.HTML
            replyMarkup = keyboard
        }
        runCatching { messageMetrics.track(MessageType.LOAN, chatId) { execute(message) } }.onFailure { log.warn("Failed to send loan notice to {}: {}", chatId, it.message) }
    }
}
