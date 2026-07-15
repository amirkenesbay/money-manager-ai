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
    LIST_CATEGORIES("listCategories"),

    // Groups
    LIST_GROUPS("listGroups"),
    SWITCH_GROUP("switchGroup"),

    // Balance / reports / history
    SHOW_BALANCE("showBalance"),
    SHOW_REPORT("showReport"),
    SHOW_HISTORY("showHistory"),

    // Notifications
    LIST_NOTIFICATIONS("listNotifications"),
    CREATE_NOTIFICATION("createNotification"),
    DELETE_NOTIFICATION("deleteNotification");

    companion object {
        fun from(name: String?): GeminiFunction? =
            entries.firstOrNull { it.functionName == name }
    }
}
