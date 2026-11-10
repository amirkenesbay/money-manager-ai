package ai.moneymanager.domain.model.nlp.arguments

data class CreateGroupArgs(
    val groupName: String
)

data class AddExpenseArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null,
    val suggestedCategoryIcon: String? = null,
    val operationDate: String? = null,
    val repeatUnit: String? = null,
    val repeatInterval: Double? = null,
    val repeatPhrase: String? = null,
    val paidBy: String? = null,
    val splitWith: String? = null,
    val currency: String? = null,
)

data class AddIncomeArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null,
    val suggestedCategoryIcon: String? = null,
    val operationDate: String? = null,
    val repeatUnit: String? = null,
    val repeatInterval: Double? = null,
    val repeatPhrase: String? = null,
    val currency: String? = null,
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

data class AskFinanceArgs(
    val intent: String,
    val operationType: String? = null,
    val categories: String? = null,
    val period: String? = null,
    val month: String? = null,
    val from: String? = null,
    val to: String? = null,
    val comparePeriod: String? = null,
    val compareMonth: String? = null,
    val member: String? = null,
    val place: String? = null,
    val tag: String? = null,
    val minAmount: Double? = null,
    val limit: Double? = null,
    val amount: Double? = null,
)

data class AddLoanArgs(
    val direction: String,
    val person: String,
    val amount: Double,
    val dueDate: String? = null,
    val note: String? = null,
)

data class RepayLoanArgs(
    val direction: String,
    val person: String,
    val amount: Double? = null,
)
