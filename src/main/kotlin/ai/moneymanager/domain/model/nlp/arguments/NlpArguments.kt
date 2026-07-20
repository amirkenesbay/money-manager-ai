package ai.moneymanager.domain.model.nlp.arguments

data class CreateGroupArgs(
    val groupName: String
)

data class AddExpenseArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null,
    val suggestedCategoryIcon: String? = null,
    val operationDate: String? = null
)

data class AddIncomeArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null,
    val suggestedCategoryIcon: String? = null,
    val operationDate: String? = null
)

data class DeleteGroupArgs(
    val groupName: String
)

data class OutOfContextArgs(
    val originalMessage: String
)

data class SwitchGroupArgs(
    val groupName: String
)

data class ShowReportArgs(
    val month: Double? = null,
    val year: Double? = null
)

data class ShowHistoryArgs(
    val startDate: String? = null,
    val endDate: String? = null,
    val type: String? = null,
    val categoryFilter: String? = null
)

data class CreateNotificationArgs(
    val name: String,
    val hour: Double,
    val minute: Double? = null
)

data class DeleteNotificationArgs(
    val name: String
)

data class DeleteLastOperationArgs(
    val type: String? = null
)

data class EditLastOperationArgs(
    val type: String? = null,
    val newAmount: Double? = null,
    val newCategory: String? = null,
    val newOperationDate: String? = null
)