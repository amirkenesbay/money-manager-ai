package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.ai.handler.AiPreparationResult
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiFeedback
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.withAiFeedback
import gcardone.junidecode.Junidecode
import kz.rmr.chatmachinist.model.ActionContext
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update

internal val aiLog = LoggerFactory.getLogger("AiTransitions")

internal const val MAX_VOICE_DURATION_SECONDS = 180

fun matchesEntityName(entityName: String, searchName: String): Boolean {
    val normalizedEntity = entityName.lowercase().trim()
    val normalizedSearch = searchName.lowercase().trim()

    if (normalizedEntity == normalizedSearch) return true

    val translitEntity = Junidecode.unidecode(normalizedEntity).lowercase()
    val translitSearch = Junidecode.unidecode(normalizedSearch).lowercase()

    return translitEntity == translitSearch
}

internal fun clearAiContext(context: MoneyManagerContext) {
    context.pendingAiAction = null
    context.aiResultMessage = null
    context.aiRedirectState = null
}

internal fun handleAiText(
    update: Update,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService,
    feedback: AiFeedback
) {
    val userMessage = update.message?.text ?: return
    val lang = context.userInfo?.language
    aiLog.info("🧠 AI processing text: $userMessage")
    feedback.show(localizationService.t("ai.feedback.thinking", lang))
    val command = commandParserService.parseCommand(userMessage)
    showErrorStageIfNeeded(command, feedback, localizationService, lang)
    processAiCommand(command, context, domainHandlers, geminiService, localizationService, aiPromptService)
}

internal fun handleAiVoice(
    update: Update,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService,
    feedback: AiFeedback
) {
    val voice = update.message?.voice ?: return
    val lang = context.userInfo?.language
    aiLog.info("🎤 AI processing voice: ${voice.duration}s")

    if (voice.duration > MAX_VOICE_DURATION_SECONDS) {
        clearAiContext(context)
        context.aiResultMessage = localizationService.t("ai.error.voice_too_long", lang, voice.duration)
        return
    }

    feedback.show(localizationService.t("ai.feedback.voice_received", lang))
    val audioBytes = telegramFileService.downloadVoice(voice)
    if (audioBytes == null) {
        clearAiContext(context)
        context.aiResultMessage = localizationService.t("ai.error.voice_download", lang)
        return
    }

    feedback.show(localizationService.t("ai.feedback.voice_processing", lang))
    val command = commandParserService.parseVoiceCommand(audioBytes)
    showErrorStageIfNeeded(command, feedback, localizationService, lang)
    processAiCommand(command, context, domainHandlers, geminiService, localizationService, aiPromptService)
}

internal fun ActionContext<MoneyManagerState, MoneyManagerContext>.processAiText(
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService
) {
    val chatId = update.message?.chatId ?: return
    telegramFileService.withAiFeedback(chatId) { feedback ->
        handleAiText(update, context, commandParserService, domainHandlers, geminiService, localizationService, aiPromptService, feedback)
    }
}

internal fun ActionContext<MoneyManagerState, MoneyManagerContext>.processAiVoice(
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService
) {
    val chatId = update.message?.chatId ?: return
    telegramFileService.withAiFeedback(chatId) { feedback ->
        handleAiVoice(
            update,
            context,
            commandParserService,
            telegramFileService,
            domainHandlers,
            geminiService,
            localizationService,
            aiPromptService,
            feedback
        )
    }
}

private fun showErrorStageIfNeeded(
    command: BotCommand,
    feedback: AiFeedback,
    localizationService: LocalizationService,
    language: String?
) {
    when (command) {
        is BotCommand.RateLimitError -> feedback.show(localizationService.t("ai.feedback.rate_limit", language))
        is BotCommand.ServiceError -> feedback.show(localizationService.t("ai.feedback.service_error", language))
        else -> Unit
    }
}

internal fun processAiCommand(
    command: BotCommand,
    context: MoneyManagerContext,
    handlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService
) {
    val lang = context.userInfo?.language
    clearAiContext(context)

    when (command) {
        is BotCommand.OutOfContext -> {
            val replyLanguage = java.util.Locale.of(lang ?: LocalizationService.FALLBACK_LANGUAGE)
                .getDisplayLanguage(java.util.Locale.ENGLISH)
            val prompt = aiPromptService.outOfContextPrompt(command.originalMessage, replyLanguage)
            val response = geminiService.generateText(prompt)
            context.aiResultMessage = response ?: localizationService.t("ai.error.out_of_context_fallback", lang)
            aiLog.info("⚠️ AI: Out of context, dynamic=${response != null}, replyLanguage=$replyLanguage")
            return
        }

        is BotCommand.ParseError -> {
            context.aiResultMessage = localizationService.t("ai.error.parse", lang)
            aiLog.info("❌ AI ParseError: ${command.error}")
            return
        }

        is BotCommand.RateLimitError -> {
            context.aiResultMessage = command.retryAfterSeconds
                ?.let { localizationService.t("ai.error.rate_limit_with_hint", lang, it) }
                ?: localizationService.t("ai.error.rate_limit", lang)
            aiLog.info("⏳ AI rate limit, retryAfter=${command.retryAfterSeconds}s")
            return
        }

        is BotCommand.ServiceError -> {
            context.aiResultMessage = localizationService.t("ai.error.service", lang)
            aiLog.info("⚠️ AI service error")
            return
        }

        else -> Unit
    }

    val handler = handlers.firstOrNull { it.canHandle(command) }
    if (handler == null) {
        context.aiResultMessage = localizationService.t("ai.error.unhandled", lang)
        aiLog.info("⚠️ AI: no handler for $command")
        return
    }

    when (val result = handler.prepareAction(command, context)) {
        is AiPreparationResult.RequiresConfirmation -> {
            context.pendingAiAction = result.action
            aiLog.info("✅ AI prepared action: ${result.action.describe(localizationService, lang)}")
        }

        is AiPreparationResult.ImmediateResult -> {
            context.aiResultMessage = result.message
            aiLog.info("✅ AI immediate result: ${result.message.take(80)}")
        }

        is AiPreparationResult.StateRedirect -> {
            context.aiRedirectState = result.state
            aiLog.info("✅ AI redirect to ${result.state}")
        }
    }
}
