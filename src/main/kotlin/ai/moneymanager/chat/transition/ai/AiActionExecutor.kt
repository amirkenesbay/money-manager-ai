package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.ai.handler.AiPreparationResult
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.LocalizationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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
        context.aiResultMessage = null
        context.aiRedirectState = null
        // aiCategoriesCache is rewritten per request in AiRequestHandler; keep it for picker rendering.
    }
}
