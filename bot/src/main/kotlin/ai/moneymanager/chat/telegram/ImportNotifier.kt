package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.model.imports.ImportPreview
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.imports.ImportService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

private val IMPORT_EXTENSIONS = setOf(".csv", ".xlsx", ".txt")
private const val READY_KEY = "import.bot.ready"
private const val COLUMNS_KEY = "import.bot.columns"
private const val FAILED_KEY = "import.bot.failed"
private const val NO_GROUP_KEY = "ai.error.no_active_group"
private const val OPEN_BUTTON_KEY = "import.bot.open"
private const val SOURCE_KEY_PREFIX = "import.source."
private const val FILE_SOURCE_KEY = "import.source.file"
private val NAMED_SOURCES = setOf(ImportSource.ZENMONEY, ImportSource.COINKEEPER, ImportSource.MONEY_MANAGER)

fun isImportFile(document: Document?): Boolean =
    document?.fileName?.lowercase()?.let { name -> IMPORT_EXTENSIONS.any { name.endsWith(it) } } == true

@Service
class ImportNotifier(
    private val importService: ImportService,
    private val telegramFileService: TelegramFileService,
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun receive(chatId: Long, userId: Long, document: Document) {
        val user = userInfoService.getUserInfoByTelegramId(userId)
        val language = user?.language
        val group = user?.activeGroupId?.let { groupService.getGroup(it) } ?: return send(chatId, t(NO_GROUP_KEY, language), null)
        val preview = telegramFileService.downloadDocument(document)
            ?.let { bytes -> runCatching { importService.read(userId, group, bytes) }.getOrNull() }
            ?: return send(chatId, t(FAILED_KEY, language), null)
        val open = miniAppGateway.button(OPEN_BUTTON_KEY, language, MiniAppScreen.IMPORT, importToken = preview.token)
        send(chatId, textOf(preview, language), InlineKeyboardMarkup(listOf(listOf(open))))
    }

    private fun textOf(preview: ImportPreview, language: String?): String {
        val from = preview.from ?: return t(COLUMNS_KEY, language)
        val to = checkNotNull(preview.to)
        return t(READY_KEY, language, sourceOf(preview.source, language), preview.count, from.format(shortDateFormatter), to.format(shortDateFormatter))
    }

    private fun sourceOf(source: ImportSource?, language: String?): String =
        source?.takeIf { it in NAMED_SOURCES }
            ?.let { t(SOURCE_KEY_PREFIX + it.name.lowercase(), language) }
            ?: t(FILE_SOURCE_KEY, language)

    private fun send(chatId: Long, text: String, keyboard: InlineKeyboardMarkup?) {
        val message = SendMessage(chatId.toString(), text).apply {
            parseMode = ParseMode.HTML
            replyMarkup = keyboard
        }
        runCatching { execute(message) }.onFailure { log.warn("Failed to send import notice to {}: {}", chatId, it.message) }
    }

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
