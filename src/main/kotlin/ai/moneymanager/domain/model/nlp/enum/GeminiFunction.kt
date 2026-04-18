package ai.moneymanager.domain.model.nlp.enum

enum class GeminiFunction(val functionName: String) {
    CREATE_GROUP("createGroup"),
    DELETE_GROUP("deleteGroup"),
    ADD_EXPENSE("addExpense"),
    ADD_INCOME("addIncome"),
    OUT_OF_CONTEXT("outOfContext"),

    // Category
    CREATE_CATEGORY("createCategory"),
    DELETE_CATEGORY("deleteCategory"),
    RENAME_CATEGORY("renameCategory"),
    CHANGE_CATEGORY_ICON("changeCategoryIcon"),
    DELETE_ALL_CATEGORIES("deleteAllCategories"),
    LIST_CATEGORIES("listCategories");

    companion object {
        fun from(name: String?): GeminiFunction? =
            entries.firstOrNull { it.functionName == name }
    }
}
