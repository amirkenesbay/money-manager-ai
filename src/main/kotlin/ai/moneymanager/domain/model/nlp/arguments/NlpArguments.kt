package ai.moneymanager.domain.model.nlp.arguments

data class CreateGroupArgs(
    val groupName: String
)

data class AddExpenseArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null
)

data class AddIncomeArgs(
    val amount: Double,
    val category: String? = null,
    val description: String? = null
)

data class DeleteGroupArgs(
    val groupName: String
)

data class OutOfContextArgs(
    val originalMessage: String
)