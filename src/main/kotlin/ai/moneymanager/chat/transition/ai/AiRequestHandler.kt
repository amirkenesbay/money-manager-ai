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

private const val MAX_VOICE_DURATION_SECONDS = 180

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
        log.info("🧠 AI processing text: $userMessage")
        feedback.show(localizationService.t("ai.feedback.thinking", lang))
        val categoryContext = loadAndCacheCategoryContext(context)
        val commands = commandParserService.parseCommands(userMessage, categoryContext)
        showErrorStageIfNeeded(commands, feedback, lang)
        actionExecutor.processCommands(commands, context)
    }

    private fun handleVoice(update: Update, context: MoneyManagerContext, feedback: AiFeedback) {
        val voice = update.message?.voice ?: return
        val lang = context.userInfo?.language
        log.info("🎤 AI processing voice: ${voice.duration}s")

        if (voice.duration > MAX_VOICE_DURATION_SECONDS) {
            actionExecutor.clear(context)
            context.aiResultMessage = localizationService.t("ai.error.voice_too_long", lang, voice.duration)
            return
        }

        feedback.show(localizationService.t("ai.feedback.voice_received", lang))
        val audioBytes = telegramFileService.downloadVoice(voice)
        if (audioBytes == null) {
            actionExecutor.clear(context)
            context.aiResultMessage = localizationService.t("ai.error.voice_download", lang)
            return
        }

        feedback.show(localizationService.t("ai.feedback.voice_processing", lang))
        val categoryContext = loadAndCacheCategoryContext(context)
        val commands = commandParserService.parseVoiceCommands(audioBytes, categoryContext)
        showErrorStageIfNeeded(commands, feedback, lang)
        actionExecutor.processCommands(commands, context)
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
                feedback.show(localizationService.t("ai.feedback.rate_limit", language))
            commands.any { it is BotCommand.ServiceError } ->
                feedback.show(localizationService.t("ai.feedback.service_error", language))
        }
    }
}
