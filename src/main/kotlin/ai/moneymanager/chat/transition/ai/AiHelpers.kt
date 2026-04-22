package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.ai.handler.AiPreparationResult
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiFeedback
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.withAiFeedback
import gcardone.junidecode.Junidecode
import kz.rmr.chatmachinist.model.ActionContext
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update

internal val aiLog = LoggerFactory.getLogger("AiTransitions")

internal const val MAX_VOICE_DURATION_SECONDS = 180

internal const val AI_MODE_INTRO = """🤖 AI Ассистент

Управляй финансами естественной речью.

🎤 Лучше всего — голосовые сообщения!
Нажми и удерживай микрофон 👇

Или напиши текстом. Сейчас я умею:

📂 Категории:
• «Создай категорию Кино»
• «Удали категорию Такси»
• «Переименуй Продукты в Еда»
• «Замени иконку Зарплата на 💵»
• «Покажи категории расходов»
• «Удали все категории»

Скоро добавлю: группы, финансы, отчёты, баланс, уведомления."""

internal const val VOICE_TOO_LONG_MESSAGE = "⚠️ Голосовое сообщение слишком длинное (%dс). Максимум 3 минуты."
internal const val VOICE_DOWNLOAD_ERROR_MESSAGE = "❌ Не удалось загрузить голосовое сообщение. Попробуй ещё раз."
internal const val PARSE_ERROR_MESSAGE =
    "❌ Не смог разобрать запрос. Попробуй переформулировать или нажми голосовую кнопку."
internal const val OUT_OF_CONTEXT_FALLBACK = """🤖 Я ассистент для финансов.

Могу помочь с категориями, группами и учётом расходов/доходов. Попробуй:
• «Создай категорию Кино»
• «Покажи категории расходов»"""
internal const val UNHANDLED_COMMAND_MESSAGE = "🛠 Этот тип команд ещё в разработке. Скоро появится!"

internal const val FEEDBACK_THINKING = "🧠 Думаю…"
internal const val FEEDBACK_VOICE_RECEIVED = "🎤 Услышал голосовое…"
internal const val FEEDBACK_VOICE_PROCESSING = "🧠 Распознаю и думаю…"
internal const val FEEDBACK_RATE_LIMIT = "❌ Лимит AI"
internal const val FEEDBACK_SERVICE_ERROR = "❌ AI недоступен"

internal const val RATE_LIMIT_WITH_HINT_MESSAGE = "⏳ Лимит AI исчерпан. Попробуй через ~%d сек."
internal const val RATE_LIMIT_MESSAGE = "⏳ Лимит AI исчерпан. Попробуй через минуту."
internal const val SERVICE_ERROR_MESSAGE = "⚠️ AI сейчас недоступен. Попробуй через пару минут."

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
    feedback: AiFeedback
) {
    val userMessage = update.message?.text ?: return
    aiLog.info("🧠 AI processing text: $userMessage")
    feedback.show(FEEDBACK_THINKING)
    val command = commandParserService.parseCommand(userMessage)
    showErrorStageIfNeeded(command, feedback)
    processAiCommand(command, context, domainHandlers, geminiService)
}

internal fun handleAiVoice(
    update: Update,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService,
    feedback: AiFeedback
) {
    val voice = update.message?.voice ?: return
    aiLog.info("🎤 AI processing voice: ${voice.duration}s")

    if (voice.duration > MAX_VOICE_DURATION_SECONDS) {
        clearAiContext(context)
        context.aiResultMessage = VOICE_TOO_LONG_MESSAGE.format(voice.duration)
        return
    }

    feedback.show(FEEDBACK_VOICE_RECEIVED)
    val audioBytes = telegramFileService.downloadVoice(voice)
    if (audioBytes == null) {
        clearAiContext(context)
        context.aiResultMessage = VOICE_DOWNLOAD_ERROR_MESSAGE
        return
    }

    feedback.show(FEEDBACK_VOICE_PROCESSING)
    val command = commandParserService.parseVoiceCommand(audioBytes)
    showErrorStageIfNeeded(command, feedback)
    processAiCommand(command, context, domainHandlers, geminiService)
}

internal fun ActionContext<MoneyManagerState, MoneyManagerContext>.processAiText(
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService
) {
    val chatId = update.message?.chatId ?: return
    telegramFileService.withAiFeedback(chatId) { feedback ->
        handleAiText(update, context, commandParserService, domainHandlers, geminiService, feedback)
    }
}

internal fun ActionContext<MoneyManagerState, MoneyManagerContext>.processAiVoice(
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    domainHandlers: List<AiDomainHandler>,
    geminiService: GeminiService
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
            feedback
        )
    }
}

private fun showErrorStageIfNeeded(command: BotCommand, feedback: AiFeedback) {
    when (command) {
        is BotCommand.RateLimitError -> feedback.show(FEEDBACK_RATE_LIMIT)
        is BotCommand.ServiceError -> feedback.show(FEEDBACK_SERVICE_ERROR)
        else -> Unit
    }
}

internal fun processAiCommand(
    command: BotCommand,
    context: MoneyManagerContext,
    handlers: List<AiDomainHandler>,
    geminiService: GeminiService
) {
    clearAiContext(context)

    when (command) {
        is BotCommand.OutOfContext -> {
            val prompt = """Ты — дружелюбный ассистент Telegram-бота по учёту финансов.
Пользователь написал: "${command.originalMessage}"
Ответь кратко и по-дружески. Если вопрос не по финансам — мягко объясни, что можешь помочь с учётом расходов, доходов, категорий и групп.
Используй эмодзи. Максимум 3-4 предложения. Не используй markdown."""
            val response = geminiService.generateText(prompt)
            context.aiResultMessage = response ?: OUT_OF_CONTEXT_FALLBACK
            aiLog.info("⚠️ AI: Out of context, dynamic=${response != null}")
            return
        }

        is BotCommand.ParseError -> {
            context.aiResultMessage = PARSE_ERROR_MESSAGE
            aiLog.info("❌ AI ParseError: ${command.error}")
            return
        }

        is BotCommand.RateLimitError -> {
            context.aiResultMessage = command.retryAfterSeconds
                ?.let { RATE_LIMIT_WITH_HINT_MESSAGE.format(it) }
                ?: RATE_LIMIT_MESSAGE
            aiLog.info("⏳ AI rate limit, retryAfter=${command.retryAfterSeconds}s")
            return
        }

        is BotCommand.ServiceError -> {
            context.aiResultMessage = SERVICE_ERROR_MESSAGE
            aiLog.info("⚠️ AI service error")
            return
        }

        else -> Unit
    }

    val handler = handlers.firstOrNull { it.canHandle(command) }
    if (handler == null) {
        context.aiResultMessage = UNHANDLED_COMMAND_MESSAGE
        aiLog.info("⚠️ AI: no handler for $command")
        return
    }

    when (val result = handler.prepareAction(command, context)) {
        is AiPreparationResult.RequiresConfirmation -> {
            context.pendingAiAction = result.action
            aiLog.info("✅ AI prepared action: ${result.action.confirmDescription}")
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
