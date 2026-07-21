package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.ai.handler.AiPreparationResult
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.LocalizationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Locale

private const val MAX_BATCH_OPERATIONS = 10
private const val BATCH_RESULT_SEPARATOR = "\n"
private const val IMMEDIATE_RESULT_LOG_PREVIEW = 80

private const val KEY_ERROR_PARSE = "ai.error.parse"
private const val KEY_ERROR_UNHANDLED = "ai.error.unhandled"
private const val KEY_ERROR_SERVICE = "ai.error.service"
private const val KEY_ERROR_RATE_LIMIT = "ai.error.rate_limit"
private const val KEY_ERROR_RATE_LIMIT_HINT = "ai.error.rate_limit_with_hint"
private const val KEY_ERROR_OUT_OF_CONTEXT_FALLBACK = "ai.error.out_of_context_fallback"
private const val KEY_BATCH_RESULT_TITLE = "ai.batch.result.title"
private const val KEY_BATCH_LIMIT_EXCEEDED = "ai.batch.limit_exceeded"
private const val KEY_BATCH_SKIPPED_OUT_OF_CONTEXT = "ai.batch.skipped_out_of_context"
private const val KEY_BATCH_SKIPPED_UNSUPPORTED = "ai.batch.skipped_unsupported"

@Component
class AiActionExecutor(
    private val localizationService: LocalizationService,
    private val geminiService: GeminiService,
    private val aiPromptService: AiPromptService,
    private val domainHandlers: List<AiDomainHandler>
) {
    private val log = LoggerFactory.getLogger(AiActionExecutor::class.java)

    // ========== EXECUTE (confirmed actions) ==========

    fun execute(context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        val action = context.pendingAiAction
        if (action == null) {
            context.aiResultMessage = localizationService.t(KEY_ERROR_PARSE, lang)
            return
        }
        context.aiResultMessage = executeAction(action, context, lang)
        context.pendingAiAction = null
    }

    fun executeBatch(context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        val actions = context.pendingAiActions
        if (actions.isEmpty()) {
            context.aiResultMessage = localizationService.t(KEY_ERROR_PARSE, lang)
            return
        }
        val receipts = actions.map { executeAction(it, context, lang) }
        context.aiResultMessage = localizationService.t(
            KEY_BATCH_RESULT_TITLE,
            lang,
            receipts.joinToString(BATCH_RESULT_SEPARATOR)
        )
        context.pendingAiActions = emptyList()
        context.aiBatchNotes = emptyList()
    }

    private fun executeAction(action: AiPendingAction, context: MoneyManagerContext, lang: String?): String =
        domainHandlers.firstOrNull { it.canExecute(action) }
            ?.execute(action, context)
            ?: localizationService.t(KEY_ERROR_UNHANDLED, lang)

    fun processCommand(command: BotCommand, context: MoneyManagerContext) {
        val lang = context.userInfo?.language
        clear(context)

        val serviceIssue = serviceIssueMessage(command, lang)
        if (serviceIssue != null) {
            context.aiResultMessage = serviceIssue
            return
        }
        if (command is BotCommand.OutOfContext) {
            context.aiResultMessage = outOfContextReply(command, lang)
            return
        }
        dispatchToHandler(command, context, lang)
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
            notes += localizationService.t(KEY_BATCH_LIMIT_EXCEEDED, lang, MAX_BATCH_OPERATIONS)
        }
        log.info("AI batch: ${commands.size} commands → ${actions.size} actions, ${notes.size} notes")
        applyBatchOutcome(actions, notes, context, lang)
    }

    fun clear(context: MoneyManagerContext) {
        context.pendingAiAction = null
        context.pendingAiActions = emptyList()
        context.aiBatchNotes = emptyList()
        context.aiResultMessage = null
        context.aiRedirectState = null
        // aiCategoriesCache is rewritten per request in AiRequestHandler; keep it for picker rendering.
    }

    private fun serviceIssueMessage(command: BotCommand, lang: String?): String? = when (command) {
        is BotCommand.ParseError -> {
            log.info("AI ParseError: ${command.error}")
            localizationService.t(KEY_ERROR_PARSE, lang)
        }

        is BotCommand.RateLimitError -> {
            log.info("AI rate limit, retryAfter=${command.retryAfterSeconds}s")
            command.retryAfterSeconds
                ?.let { localizationService.t(KEY_ERROR_RATE_LIMIT_HINT, lang, it) }
                ?: localizationService.t(KEY_ERROR_RATE_LIMIT, lang)
        }

        is BotCommand.ServiceError -> {
            log.info("AI service error")
            localizationService.t(KEY_ERROR_SERVICE, lang)
        }

        else -> null
    }

    private fun outOfContextReply(command: BotCommand.OutOfContext, lang: String?): String {
        val replyLanguage = Locale.of(lang ?: LocalizationService.FALLBACK_LANGUAGE)
            .getDisplayLanguage(Locale.ENGLISH)
        val prompt = aiPromptService.outOfContextPrompt(command.originalMessage, replyLanguage)
        val response = geminiService.generateText(prompt)
        log.info("AI out of context, dynamic=${response != null}, replyLanguage=$replyLanguage")
        return response?.let { escapeHtml(it) } ?: localizationService.t(KEY_ERROR_OUT_OF_CONTEXT_FALLBACK, lang)
    }

    private fun dispatchToHandler(command: BotCommand, context: MoneyManagerContext, lang: String?) {
        val handler = domainHandlers.firstOrNull { it.canHandle(command) }
        if (handler == null) {
            log.info("AI: no handler for $command")
            context.aiResultMessage = localizationService.t(KEY_ERROR_UNHANDLED, lang)
            return
        }
        applyPreparationResult(handler.prepareAction(command, context), context, lang)
    }

    private fun applyPreparationResult(
        result: AiPreparationResult,
        context: MoneyManagerContext,
        lang: String?
    ) {
        when (result) {
            is AiPreparationResult.RequiresConfirmation -> {
                context.pendingAiAction = result.action
                log.info("AI prepared action: ${result.action.describe(localizationService, Currency.DEFAULT, lang)}")
            }

            is AiPreparationResult.ImmediateResult -> {
                context.aiResultMessage = result.message
                log.info("AI immediate result: ${result.message.take(IMMEDIATE_RESULT_LOG_PREVIEW)}")
            }

            is AiPreparationResult.StateRedirect -> {
                context.aiRedirectState = result.state
                log.info("AI redirect to ${result.state}")
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
            notes += localizationService.t(KEY_BATCH_SKIPPED_OUT_OF_CONTEXT, lang)
            return
        }
        val handler = domainHandlers.firstOrNull { it.canHandle(command) }
        if (handler == null) {
            notes += localizationService.t(KEY_BATCH_SKIPPED_UNSUPPORTED, lang)
            return
        }
        when (val result = handler.prepareAction(command, context)) {
            is AiPreparationResult.RequiresConfirmation -> actions += result.action
            is AiPreparationResult.ImmediateResult -> notes += result.message
            is AiPreparationResult.StateRedirect ->
                notes += localizationService.t(KEY_BATCH_SKIPPED_UNSUPPORTED, lang)
        }
    }

    private fun applyBatchOutcome(
        actions: List<AiPendingAction>,
        notes: List<String>,
        context: MoneyManagerContext,
        lang: String?
    ) {
        val distinctNotes = notes.distinct()
        when {
            actions.isEmpty() -> context.aiResultMessage = distinctNotes
                .joinToString(BATCH_RESULT_SEPARATOR)
                .ifEmpty { localizationService.t(KEY_ERROR_PARSE, lang) }

            actions.size == 1 && distinctNotes.isEmpty() -> context.pendingAiAction = actions.first()

            else -> {
                context.pendingAiActions = actions
                context.aiBatchNotes = distinctNotes
            }
        }
    }
}
