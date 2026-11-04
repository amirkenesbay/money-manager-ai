package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.event.ExportReady
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.ByteArrayInputStream

private const val PERIOD_SEPARATOR = " — "

@Service
class ExportSender(
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    @EventListener
    fun onExportReady(event: ExportReady) {
        val language = userInfoService.getUserInfoByTelegramId(event.telegramUserId)?.language
        execute(SendDocument(event.telegramUserId.toString(), InputFile(ByteArrayInputStream(event.content), event.fileName)).apply {
            caption = localizationService.t("export.caption", language, escapeHtml(event.groupName), period(event, language), event.count)
            parseMode = ParseMode.HTML
        })
    }

    private fun period(event: ExportReady, language: String?): String {
        val from = event.from ?: return localizationService.t("export.caption.all_time", language)
        return from.format(shortDateFormatter) + PERIOD_SEPARATOR + (event.to?.format(shortDateFormatter) ?: "")
    }
}
