package ai.moneymanager.domain.model.nlp

/**
 * Результат парсинга пользовательского сообщения через Gemini
 */
sealed class BotCommand {

    /**
     * Команда создания группы
     */
    data class CreateGroup(
        val groupName: String
    ) : BotCommand()

    /**
     * Команда удаления группы
     */
    data class DeleteGroup(
        val groupName: String
    ) : BotCommand()

    /**
     * Команда добавления расхода
     */
    data class AddExpense(
        val amount: Double,
        val category: String?,
        val description: String?
    ) : BotCommand()

    /**
     * Команда добавления дохода
     */
    data class AddIncome(
        val amount: Double,
        val category: String?,
        val description: String?
    ) : BotCommand()

    /**
     * Сообщение вне контекста бота
     */
    data class OutOfContext(
        val originalMessage: String
    ) : BotCommand()

    /**
     * Ошибка парсинга
     */
    data class ParseError(
        val error: String
    ) : BotCommand()

    /**
     * Превышен лимит запросов к Gemini (HTTP 429).
     * retryAfterSeconds — рекомендуемая пауза из тела ответа, если смогли распарсить.
     */
    data class RateLimitError(
        val retryAfterSeconds: Long?
    ) : BotCommand()

    /**
     * Gemini временно недоступен: 5xx, сетевые ошибки или прочие 4xx кроме 429.
     */
    object ServiceError : BotCommand()

    // ===== CATEGORY =====

    data class CreateCategory(
        val name: String,
        val type: String,
        val icon: String?
    ) : BotCommand()

    data class DeleteCategory(
        val name: String,
        val type: String?
    ) : BotCommand()

    data class RenameCategory(
        val oldName: String,
        val newName: String,
        val type: String?
    ) : BotCommand()

    data class ChangeCategoryIcon(
        val name: String,
        val newIcon: String,
        val type: String?
    ) : BotCommand()

    object DeleteAllCategories : BotCommand()

    data class ListCategories(
        val type: String?
    ) : BotCommand()
}