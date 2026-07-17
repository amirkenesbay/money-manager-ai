package ai.moneymanager.chat.transition.ai

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiFeedback
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.withAiFeedback
import kz.rmr.chatmachinist.model.ActionContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.Voice

private const val MAX_VOICE_DURATION_SECONDS = 180

private const val KEY_FEEDBACK_THINKING = "ai.feedback.thinking"
private const val KEY_FEEDBACK_VOICE_RECEIVED = "ai.feedback.voice_received"
private const val KEY_FEEDBACK_VOICE_PROCESSING = "ai.feedback.voice_processing"
private const val KEY_FEEDBACK_RATE_LIMIT = "ai.feedback.rate_limit"
private const val KEY_FEEDBACK_SERVICE_ERROR = "ai.feedback.service_error"
private const val KEY_ERROR_VOICE_TOO_LONG = "ai.error.voice_too_long"
private const val KEY_ERROR_VOICE_DOWNLOAD = "ai.error.voice_download"

@Component
class AiRequestHandler(
    private val commandParserService: CommandParserService,
    private val telegramFileService: TelegramFileService,
    private val categoryService: CategoryService,
    private val aiPromptService: AiPromptService,
    private val localizationService: LocalizationService,
    private val actionExecutor: AiActionExecutor
) {
    private val log = LoggerFactory.getLogger(AiRequestHandler::class.java)

    fun processText(action: ActionContext<MoneyManagerState, MoneyManagerContext>) {
        val chatId = action.update.message?.chatId ?: return
        telegramFileService.withAiFeedback(chatId) { feedback ->
            handleText(action.update, action.context, feedback)
        }
    }

    fun processVoice(action: ActionContext<MoneyManagerState, MoneyManagerContext>) {
        val chatId = action.update.message?.chatId ?: return
        telegramFileService.withAiFeedback(chatId) { feedback ->
            handleVoice(action.update, action.context, feedback)
        }
    }

    private fun handleText(update: Update, context: MoneyManagerContext, feedback: AiFeedback) {
        val userMessage = update.message?.text ?: return
        val lang = context.userInfo?.language
        log.info("AI processing text: $userMessage")
        feedback.show(localizationService.t(KEY_FEEDBACK_THINKING, lang))
        parseAndExecute(context, feedback, lang) { categoryContext ->
            commandParserService.parseCommands(userMessage, categoryContext)
        }
    }

    private fun handleVoice(update: Update, context: MoneyManagerContext, feedback: AiFeedback) {
        val voice = update.message?.voice ?: return
        val lang = context.userInfo?.language
        log.info("AI processing voice: ${voice.duration}s")
        val audioBytes = downloadVoiceOrReportError(voice, context, feedback, lang) ?: return
        feedback.show(localizationService.t(KEY_FEEDBACK_VOICE_PROCESSING, lang))
        parseAndExecute(context, feedback, lang) { categoryContext ->
            commandParserService.parseVoiceCommands(audioBytes, categoryContext)
        }
    }

    /** Общий хвост text/voice: контекст категорий → парсинг → стадия ошибки → выполнение. */
    private fun parseAndExecute(
        context: MoneyManagerContext,
        feedback: AiFeedback,
        lang: String?,
        parse: (categoryContext: String?) -> List<BotCommand>
    ) {
        val commands = parse(loadAndCacheCategoryContext(context))
        showErrorStageIfNeeded(commands, feedback, lang)
        actionExecutor.processCommands(commands, context)
    }

    /** Валидация длительности и скачивание голосового; null — ошибка уже записана в контекст. */
    private fun downloadVoiceOrReportError(
        voice: Voice,
        context: MoneyManagerContext,
        feedback: AiFeedback,
        lang: String?
    ): ByteArray? {
        if (voice.duration > MAX_VOICE_DURATION_SECONDS) {
            reportError(context, KEY_ERROR_VOICE_TOO_LONG, lang, voice.duration)
            return null
        }
        feedback.show(localizationService.t(KEY_FEEDBACK_VOICE_RECEIVED, lang))
        val audioBytes = telegramFileService.downloadVoice(voice)
        if (audioBytes == null) {
            reportError(context, KEY_ERROR_VOICE_DOWNLOAD, lang)
            return null
        }
        return audioBytes
    }

    private fun reportError(context: MoneyManagerContext, key: String, lang: String?, vararg args: Any) {
        actionExecutor.clear(context)
        context.aiResultMessage = localizationService.t(key, lang, *args)
    }

    private fun loadAndCacheCategoryContext(context: MoneyManagerContext): String? {
        val groupId = context.userInfo?.activeGroupId ?: run {
            context.aiCategoriesCache = null
            return null
        }
        val categories: List<Category> = categoryService.getCategoriesByGroup(groupId)
        context.aiCategoriesCache = categories
        return aiPromptService.categoryContextPreamble(categories)
    }

    private fun showErrorStageIfNeeded(commands: List<BotCommand>, feedback: AiFeedback, language: String?) {
        when {
            commands.any { it is BotCommand.RateLimitError } ->
                feedback.show(localizationService.t(KEY_FEEDBACK_RATE_LIMIT, language))

            commands.any { it is BotCommand.ServiceError } ->
                feedback.show(localizationService.t(KEY_FEEDBACK_SERVICE_ERROR, language))
        }
    }
}
