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
}