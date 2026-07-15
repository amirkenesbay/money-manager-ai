package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.ai.handler.AiPreparationResult
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.LocalizationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private const val MAX_BATCH_OPERATIONS = 10
private const val BATCH_RESULT_SEPARATOR = "\n"

@Component
class AiActionExecutor(
    private val localizationService: LocalizationService,
    private val geminiService: GeminiService,
    private val aiPromptService: AiPromptService,
    private val domainHandlers: List<AiDomainHandler>
) {
    private val log = LoggerFactory.getLogger(AiActionExecutor::class.java)

    fun execute(context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        val action = context.pendingAiAction
        if (action == null) {
            context.aiResultMessage = localizationService.t("ai.error.parse", lang)
            return
        }
        val handler = domainHandlers.firstOrNull { it.canExecute(action) }
        if (handler == null) {
            context.aiResultMessage = localizationService.t("ai.error.unhandled", lang)
            context.pendingAiAction = null
            return
        }
        val message = handler.execute(action, context)
        context.aiResultMessage = message
        context.pendingAiAction = null
    }

    fun executeBatch(context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        val actions = context.pendingAiActions
        if (actions.isEmpty()) {
            context.aiResultMessage = localizationService.t("ai.error.parse", lang)
            return
        }
        val messages = actions.map { action ->
            val handler = domainHandlers.firstOrNull { it.canExecute(action) }
            handler?.execute(action, context) ?: localizationService.t("ai.error.unhandled", lang)
        }
        context.aiResultMessage = localizationService.t(
            "ai.batch.result.title",
            lang,
            messages.joinToString(BATCH_RESULT_SEPARATOR)
        )
        context.pendingAiActions = emptyList()
        context.aiBatchNotes = emptyList()
    }

    fun processCommands(commands: List<BotCommand>, context: MoneyManagerContext) {
        val single = commands.singleOrNull()
        if (single != null) {
            processCommand(single, context)
            return
        }
        val lang = context.userInfo?.language
        clear(context)

        val notes = mutableListOf<String>()
        val actions = mutableListOf<AiPendingAction>()
        commands.take(MAX_BATCH_OPERATIONS).forEach { command ->
            prepareBatchItem(command, context, lang, actions, notes)
        }
        if (commands.size > MAX_BATCH_OPERATIONS) {
            notes += localizationService.t("ai.batch.limit_exceeded", lang, MAX_BATCH_OPERATIONS)
        }
        log.info("📦 AI batch: ${commands.size} commands → ${actions.size} actions, ${notes.size} notes")

        when {
            actions.isEmpty() -> context.aiResultMessage = notes
                .joinToString(BATCH_RESULT_SEPARATOR)
                .ifEmpty { localizationService.t("ai.error.parse", lang) }

            actions.size == 1 && notes.isEmpty() -> context.pendingAiAction = actions.first()

            else -> {
                context.pendingAiActions = actions
                context.aiBatchNotes = notes
            }
        }
    }

    private fun prepareBatchItem(
        command: BotCommand,
        context: MoneyManagerContext,
        lang: String?,
        actions: MutableList<AiPendingAction>,
        notes: MutableList<String>
    ) {
        if (command is BotCommand.OutOfContext) {
            notes += localizationService.t("ai.batch.skipped_out_of_context", lang)
            return
        }
        val handler = domainHandlers.firstOrNull { it.canHandle(command) }
        if (handler == null) {
            notes += localizationService.t("ai.batch.skipped_unsupported", lang)
            return
        }
        when (val result = handler.prepareAction(command, context)) {
            is AiPreparationResult.RequiresConfirmation -> actions += result.action
            is AiPreparationResult.ImmediateResult -> notes += result.message
            is AiPreparationResult.StateRedirect ->
                notes += localizationService.t("ai.batch.skipped_unsupported", lang)
        }
    }

    fun processCommand(command: BotCommand, context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        clear(context)

        when (command) {
            is BotCommand.OutOfContext -> {
                val replyLanguage = java.util.Locale.of(lang ?: LocalizationService.FALLBACK_LANGUAGE)
                    .getDisplayLanguage(java.util.Locale.ENGLISH)
                val prompt = aiPromptService.outOfContextPrompt(command.originalMessage, replyLanguage)
                val response = geminiService.generateText(prompt)
                context.aiResultMessage = response ?: localizationService.t("ai.error.out_of_context_fallback", lang)
                log.info("⚠️ AI: Out of context, dynamic=${response != null}, replyLanguage=$replyLanguage")
                return
            }

            is BotCommand.ParseError -> {
                context.aiResultMessage = localizationService.t("ai.error.parse", lang)
                log.info("❌ AI ParseError: ${command.error}")
                return
            }

            is BotCommand.RateLimitError -> {
                context.aiResultMessage = command.retryAfterSeconds
                    ?.let { localizationService.t("ai.error.rate_limit_with_hint", lang, it) }
                    ?: localizationService.t("ai.error.rate_limit", lang)
                log.info("⏳ AI rate limit, retryAfter=${command.retryAfterSeconds}s")
                return
            }

            is BotCommand.ServiceError -> {
                context.aiResultMessage = localizationService.t("ai.error.service", lang)
                log.info("⚠️ AI service error")
                return
            }

            else -> Unit
        }

        val handler = domainHandlers.firstOrNull { it.canHandle(command) }
        if (handler == null) {
            context.aiResultMessage = localizationService.t("ai.error.unhandled", lang)
            log.info("⚠️ AI: no handler for $command")
            return
        }

        when (val result = handler.prepareAction(command, context)) {
            is AiPreparationResult.RequiresConfirmation -> {
                context.pendingAiAction = result.action
                log.info("✅ AI prepared action: ${result.action.describe(localizationService, lang)}")
            }

            is AiPreparationResult.ImmediateResult -> {
                context.aiResultMessage = result.message
                log.info("✅ AI immediate result: ${result.message.take(80)}")
            }

            is AiPreparationResult.StateRedirect -> {
                context.aiRedirectState = result.state
                log.info("✅ AI redirect to ${result.state}")
            }
        }
    }

    fun clear(context: MoneyManagerContext) {
        context.pendingAiAction = null
        context.pendingAiActions = emptyList()
        context.aiBatchNotes = emptyList()
        context.aiResultMessage = null
        context.aiRedirectState = null
        // aiCategoriesCache is rewritten per request in AiRequestHandler; keep it for picker rendering.
    }
}
