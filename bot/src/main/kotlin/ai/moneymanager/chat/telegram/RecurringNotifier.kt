package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.DOT_SEPARATOR
import ai.moneymanager.metrics.MessageType
import ai.moneymanager.metrics.MessageMetrics
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.RecordedOccurrence
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.UserInfoService
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

private const val RECURRING_CHECK_INTERVAL_MS = 300_000L
private const val LOOKAHEAD_DAYS = 2L
private val RECORD_TIME: LocalTime = LocalTime.of(10, 0)
private val HEADS_UP_TIME: LocalTime = LocalTime.of(19, 0)

@Service
class RecurringNotifier(
    private val recurringPaymentService: RecurringPaymentService,
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val localizationService: LocalizationService,
    private val messageMetrics: MessageMetrics,
    private val clock: Clock,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = RECURRING_CHECK_INTERVAL_MS)
    fun processDuePayments() {
        recurringPaymentService.dueBy(LocalDate.now(clock).plusDays(LOOKAHEAD_DAYS)).forEach { payment ->
            runCatching { process(payment) }
                .onFailure { log.warn("Failed to process recurring payment {}: {}", payment.id, it.message) }
        }
    }

    fun undo(chatId: Long, actorId: Long, operationId: ObjectId, language: String?) {
        val text = recurringPaymentService.undoOperation(actorId, operationId)
            ?.let { t("recurring.undone", language, operationLabel(it), amount(it, currencyOf(it.groupId))) }
            ?: t("recurring.undo_missing", language)
        send(chatId, text, null)
    }

    fun recordNow(chatId: Long, actorId: Long, request: RecurringRecordRequest, language: String?) {
        val text = recurringPaymentService.recordOccurrence(actorId, request.paymentId, request.date)
            ?.let { (_, operation) -> t("recurring.recorded_now", language, operationLabel(operation), amount(operation, currencyOf(operation.groupId))) }
            ?: t("recurring.record_failed", language)
        send(chatId, text, null)
    }

    private fun process(payment: RecurringPaymentEntity) {
        val user = userInfoService.getUserInfoByTelegramId(payment.creatorId) ?: return
        val now = LocalDateTime.now(clock.withZone(user.zone))
        val today = now.toLocalDate()
        if (!payment.nextDate.isAfter(today) && !now.toLocalTime().isBefore(RECORD_TIME)) {
            recurringPaymentService.recordDue(payment, today).forEach { occurrence -> announce(user, payment, occurrence, today) }
            return
        }
        val headsUpDue = user.recurringHeadsUp && !now.toLocalTime().isBefore(HEADS_UP_TIME)
        if (headsUpDue && recurringPaymentService.claimHeadsUp(payment, today.plusDays(1))) headsUp(user, payment)
    }

    private fun announce(user: UserInfo, payment: RecurringPaymentEntity, occurrence: RecordedOccurrence, today: LocalDate) {
        val language = user.language
        val chatId = checkNotNull(user.telegramUserId)
        val currency = currencyOf(payment.groupId)
        val operation = occurrence.operation
        if (operation != null) {
            val date = if (occurrence.date == today) "" else DOT_SEPARATOR + occurrence.date.format(shortDateFormatter)
            send(chatId, t("recurring.recorded", language, paymentLabel(payment), amount(operation, currency), date), button("recurring.button.undo", language, undoOperationParameter(checkNotNull(operation.id))))
            return
        }
        if (payment.mode == RecurringMode.AUTO) return
        val record = button("recurring.button.record", language, recordRecurringParameter(checkNotNull(payment.id), occurrence.date))
        send(chatId, t("recurring.remind", language, paymentLabel(payment), paymentAmount(payment, currency)), record)
    }

    private fun headsUp(user: UserInfo, payment: RecurringPaymentEntity) {
        val key = if (payment.mode == RecurringMode.AUTO) "recurring.heads_up.auto" else "recurring.heads_up.remind"
        send(checkNotNull(user.telegramUserId), t(key, user.language, paymentLabel(payment), paymentAmount(payment, currencyOf(payment.groupId))), null)
    }

    private fun button(textKey: String, language: String?, parameter: String): InlineKeyboardMarkup =
        InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton(t(textKey, language)).apply { url = startLink(botUsername, parameter) })))

    private fun send(chatId: Long, text: String, keyboard: InlineKeyboardMarkup?) {
        val message = SendMessage(chatId.toString(), text).apply {
            parseMode = ParseMode.HTML
            replyMarkup = keyboard
        }
        runCatching { messageMetrics.track(MessageType.RECURRING, chatId) { execute(message) } }.onFailure { log.warn("Failed to send recurring payment notice to {}: {}", chatId, it.message) }
    }

    private fun paymentLabel(payment: RecurringPaymentEntity): String = label(payment.categoryIcon, payment.categoryName, payment.description)

    private fun operationLabel(operation: FinanceOperationEntity): String = label(operation.categoryIcon, operation.categoryName, operation.description)

    private fun label(icon: String?, category: String, description: String?): String =
        escapeHtml(formatIconPrefix(icon) + category + (description?.let { DOT_SEPARATOR + it } ?: ""))

    private fun paymentAmount(payment: RecurringPaymentEntity, currency: Currency): String = formatSignedAmount(payment.type, payment.amount, currency)

    private fun amount(operation: FinanceOperationEntity, currency: Currency): String = formatSignedAmount(operation.type, operation.amount, currency)

    private fun currencyOf(groupId: ObjectId): Currency = groupService.getGroup(groupId)?.currency ?: Currency.DEFAULT

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
