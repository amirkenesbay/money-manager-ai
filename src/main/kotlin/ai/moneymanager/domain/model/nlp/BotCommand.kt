package ai.moneymanager.domain.model.nlp

sealed class BotCommand {
    data class CreateGroup(
        val groupName: String
    ) : BotCommand()

    data class DeleteGroup(
        val groupName: String
    ) : BotCommand()

    data class AddExpense(
        val amount: Double,
        val category: String?,
        val description: String?,
        val suggestedCategoryIcon: String?,
        val operationDate: String?
    ) : BotCommand()

    data class AddIncome(
        val amount: Double,
        val category: String?,
        val description: String?,
        val suggestedCategoryIcon: String?,
        val operationDate: String?
    ) : BotCommand()

    data class OutOfContext(
        val originalMessage: String
    ) : BotCommand()

    data class ParseError(
        val error: String
    ) : BotCommand()

    data class RateLimitError(
        val retryAfterSeconds: Long?
    ) : BotCommand()

    object ServiceError : BotCommand()

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

    object ListGroups : BotCommand()

    data class SwitchGroup(
        val groupName: String
    ) : BotCommand()

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

    object ListNotifications : BotCommand()

    data class CreateNotification(
        val name: String,
        val hour: Int,
        val minute: Int
    ) : BotCommand()

    data class DeleteNotification(
        val name: String
    ) : BotCommand()

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
