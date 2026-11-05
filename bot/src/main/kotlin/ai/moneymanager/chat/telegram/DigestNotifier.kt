package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.digest.DigestFormatter
import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.dueDigestPeriod
import ai.moneymanager.service.DigestService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
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
import java.time.LocalDateTime
import java.time.YearMonth

private const val DIGEST_CHECK_INTERVAL_MS = 300_000L
private const val REPORT_BUTTON_KEY = "mini_app.button.report"
private const val OPEN_BUTTON_KEY = "mini_app.button.open"
private const val ASK_BUTTON_KEY = "mini_app.button.ask"

@Service
class DigestNotifier(
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val digestService: DigestService,
    private val digestFormatter: DigestFormatter,
    private val miniAppGateway: MiniAppGateway,
    private val clock: Clock,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
    @Value("\${chat-machinist.bot.name}")
    private val botUsername: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = DIGEST_CHECK_INTERVAL_MS)
    fun sendDueDigests() {
        userInfoService.digestRecipients().forEach { user ->
            runCatching { sendDueDigests(user) }
                .onFailure { log.warn("Failed to send digests to user {}: {}", user.telegramUserId, it.message) }
        }
    }

    fun confirmMuted(chatId: Long, kind: DigestKind, language: String?) {
        runCatching { execute(SendMessage(chatId.toString(), digestFormatter.mutedConfirmation(kind, language))) }
            .onFailure { log.warn("Failed to confirm muted {} digest in chat {}: {}", kind, chatId, it.message) }
    }

    private fun sendDueDigests(user: UserInfo) {
        val telegramUserId = checkNotNull(user.telegramUserId)
        val group = groupService.getGroup(checkNotNull(user.activeGroupId)) ?: return
        val now = LocalDateTime.now(clock.withZone(user.zone))
        (DigestKind.entries - user.mutedDigests).forEach { kind ->
            val period = dueDigestPeriod(kind, now, group.monthStartDay) ?: return@forEach
            if (digestService.wasSent(telegramUserId, period)) return@forEach
            val digest = digestService.build(group, period, now.toLocalDate()) ?: return@forEach
            if (digestService.claim(telegramUserId, period)) execute(message(telegramUserId, digest, user.language))
        }
    }

    private fun message(telegramUserId: Long, digest: Digest, language: String?): SendMessage =
        SendMessage(telegramUserId.toString(), digestFormatter.format(digest, language)).apply {
            parseMode = ParseMode.HTML
            replyMarkup = keyboard(digest, language)
        }

    private fun keyboard(digest: Digest, language: String?): InlineKeyboardMarkup {
        val kind = digest.period.kind
        val mute = InlineKeyboardButton(digestFormatter.muteButton(kind, language)).apply {
            url = startLink(botUsername, digestMuteParameter(kind))
        }
        val open = if (miniAppGateway.enabled) openButton(digest, language) else null
        val ask = if (miniAppGateway.enabled && digest.period.kind == DigestKind.MONTH) miniAppGateway.button(ASK_BUTTON_KEY, language, MiniAppScreen.ASK) else null
        return InlineKeyboardMarkup(listOfNotNull(open?.let { listOf(it) }, ask?.let { listOf(it) }, listOf(mute)))
    }

    private fun openButton(digest: Digest, language: String?): InlineKeyboardButton =
        if (digest.period.kind == DigestKind.MONTH) {
            miniAppGateway.button(REPORT_BUTTON_KEY, language, MiniAppScreen.REPORTS, YearMonth.from(digest.period.start))
        } else {
            miniAppGateway.button(OPEN_BUTTON_KEY, language, MiniAppScreen.HOME)
        }
}
