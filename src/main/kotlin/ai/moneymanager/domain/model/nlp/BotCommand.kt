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
        val description: String?,
        val suggestedCategoryIcon: String?,
        val operationDate: String?
    ) : BotCommand()

    /**
     * Команда добавления дохода
     */
    data class AddIncome(
        val amount: Double,
        val category: String?,
        val description: String?,
        val suggestedCategoryIcon: String?,
        val operationDate: String?
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

    // ===== GROUPS =====

    object ListGroups : BotCommand()

    data class SwitchGroup(
        val groupName: String
    ) : BotCommand()

    // ===== BALANCE / REPORTS / HISTORY =====

    object ShowBalance : BotCommand()

    data class ShowReport(
        val month: Int?,
        val year: Int?
    ) : BotCommand()

    data class ShowHistory(
        val startDate: String?,
        val endDate: String?,
        val type: String?,
        val categoryFilter: String?
    ) : BotCommand()

    // ===== NOTIFICATIONS =====

    object ListNotifications : BotCommand()

    data class CreateNotification(
        val name: String,
        val hour: Int,
        val minute: Int
    ) : BotCommand()

    data class DeleteNotification(
        val name: String
    ) : BotCommand()

    // ===== RECENT OPERATION EDIT =====

    data class DeleteLastOperation(
        val type: String?
    ) : BotCommand()

    data class EditLastOperation(
        val type: String?,
        val newAmount: Double?,
        val newCategory: String?,
        val newOperationDate: String?
    ) : BotCommand()
}