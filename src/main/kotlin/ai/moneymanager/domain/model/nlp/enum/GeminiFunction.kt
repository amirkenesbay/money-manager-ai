package ai.moneymanager.domain.model.nlp.enum

enum class GeminiFunction(val functionName: String) {
    CREATE_GROUP("createGroup"),
    DELETE_GROUP("deleteGroup"),
    ADD_EXPENSE("addExpense"),
    ADD_INCOME("addIncome"),
    OUT_OF_CONTEXT("outOfContext");

    companion object {
        fun from(name: String?): GeminiFunction? =
            entries.firstOrNull { it.functionName == name }
    }
}