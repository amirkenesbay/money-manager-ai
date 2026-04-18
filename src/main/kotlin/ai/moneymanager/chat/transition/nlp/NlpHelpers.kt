package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.GeminiService
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("NlpTransitions")

internal const val OUT_OF_CONTEXT_FALLBACK = """Я бот для учета финансов. Могу помочь:
• Создать группу ("создай группу друзья")
• Добавить расход ("кофе 500")
• Добавить доход ("зарплата 500000")"""

internal const val WHAT_TO_ASK_MESSAGE = """💡 Вот что можно написать:

💸 Расходы: «кофе 500», «такси 1200», «продукты 3500»
💰 Доходы: «зарплата 250 000», «подарок 10 000»
👥 Группы: «создай группу Семья», «удали группу Работа»
🤔 Вопросы: «как сэкономить?», «что такое бюджет?»

Просто напиши — отвечу!"""

internal const val VOICE_TOO_LONG_MESSAGE = "⚠️ Голосовое сообщение слишком длинное (%dс). Максимум 3 минуты."
internal const val VOICE_DOWNLOAD_ERROR_MESSAGE = "❌ Не удалось загрузить голосовое сообщение. Попробуйте еще раз."
internal const val PARSE_ERROR_MESSAGE = "Не удалось обработать сообщение. Попробуйте еще раз."

internal val BotCommand.targetState: MoneyManagerState
    get() = when (this) {
        is BotCommand.CreateGroup -> MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
        is BotCommand.DeleteGroup -> MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
        is BotCommand.OutOfContext,
        is BotCommand.AddExpense,
        is BotCommand.AddIncome,
        is BotCommand.ParseError,
        is BotCommand.CreateCategory,
        is BotCommand.DeleteCategory,
        is BotCommand.RenameCategory,
        is BotCommand.ChangeCategoryIcon,
        is BotCommand.DeleteAllCategories,
        is BotCommand.ListCategories -> MoneyManagerState.NLP_RESPONSE
    }

internal fun clearNlpContext(context: MoneyManagerContext) {
    context.nlpResponse = null
    context.nlpNewMessage = true
    context.nlpGroupName = null
    context.nlpTargetState = null
    context.nlpGroupToDelete = null
}

internal fun processNlpCommand(
    command: BotCommand,
    context: MoneyManagerContext,
    geminiService: GeminiService
) {
    clearNlpContext(context)
    context.parsedCommand = command

    when (command) {
        is BotCommand.CreateGroup -> {
            context.nlpGroupName = command.groupName
            context.nlpTargetState = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            log.info("✅ NLP parsed: CreateGroup(${command.groupName})")
        }
        is BotCommand.DeleteGroup -> {
            context.nlpGroupName = command.groupName
            context.nlpTargetState = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
            log.info("✅ NLP parsed: DeleteGroup(${command.groupName})")
        }
        is BotCommand.OutOfContext -> {
            val prompt = """Ты — дружелюбный ассистент Telegram-бота по учёту финансов.
Пользователь написал: "${command.originalMessage}"
Ответь кратко и по-дружески. Если вопрос не по финансам — мягко объясни, что можешь помочь с учётом расходов, доходов и групп.
Используй эмодзи. Максимум 3-4 предложения. Не используй markdown."""
            val response = geminiService.generateText(prompt)
            context.nlpResponse = response ?: OUT_OF_CONTEXT_FALLBACK
            context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
            log.info("⚠️ NLP: Out of context, dynamic=${response != null}")
        }
        is BotCommand.AddExpense -> {
            context.nlpResponse = """
                |Функция добавления расходов скоро будет доступна!
                |Распознано: ${command.category ?: "без категории"}, ${command.amount} тг
            """.trimMargin()
            context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
            log.info("✅ NLP parsed: AddExpense(${command.amount}, ${command.category})")
        }
        is BotCommand.AddIncome -> {
            context.nlpResponse = """
                |Функция добавления доходов скоро будет доступна!
                |Распознано: ${command.category ?: "без категории"}, ${command.amount} тг
            """.trimMargin()
            context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
            log.info("✅ NLP parsed: AddIncome(${command.amount}, ${command.category})")
        }
        is BotCommand.ParseError -> {
            context.nlpResponse = PARSE_ERROR_MESSAGE
            context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
            log.info("❌ NLP error: ${command.error}")
        }
        is BotCommand.CreateCategory,
        is BotCommand.DeleteCategory,
        is BotCommand.RenameCategory,
        is BotCommand.ChangeCategoryIcon,
        is BotCommand.DeleteAllCategories,
        is BotCommand.ListCategories -> {
            // Категориальные команды обрабатываются в новом AI-флоу.
            context.nlpResponse = PARSE_ERROR_MESSAGE
            context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
        }
    }
}