package ai.moneymanager.domain.model.nlp.enum

import ai.moneymanager.domain.model.nlp.BotFunctions
import java.lang.reflect.Method

private val STRING = String::class.java
private val NUMBER = Double::class.java
private val OPTIONAL_NUMBER = Double::class.javaObjectType

enum class GeminiFunction(val functionName: String, val inMiniApp: Boolean, private vararg val parameterTypes: Class<*>) {
    CREATE_GROUP("createGroup", false, STRING),
    DELETE_GROUP("deleteGroup", false, STRING),
    ADD_EXPENSE("addExpense", true, NUMBER, STRING, STRING, STRING, STRING, STRING, OPTIONAL_NUMBER, STRING, STRING, STRING, STRING),
    ADD_INCOME("addIncome", true, NUMBER, STRING, STRING, STRING, STRING, STRING, OPTIONAL_NUMBER, STRING, STRING),
    OUT_OF_CONTEXT("outOfContext", true, STRING),

    CREATE_CATEGORY("createCategory", false, STRING, STRING, STRING),
    DELETE_CATEGORY("deleteCategory", false, STRING, STRING),
    RENAME_CATEGORY("renameCategory", false, STRING, STRING, STRING),
    CHANGE_CATEGORY_ICON("changeCategoryIcon", false, STRING, STRING, STRING),
    DELETE_ALL_CATEGORIES("deleteAllCategories", false),
    LIST_CATEGORIES("listCategories", false, STRING),

    LIST_GROUPS("listGroups", false),
    SWITCH_GROUP("switchGroup", false, STRING),

    SHOW_BALANCE("showBalance", false),
    SHOW_REPORT("showReport", false, OPTIONAL_NUMBER, OPTIONAL_NUMBER),
    SHOW_HISTORY("showHistory", false, STRING, STRING, STRING, STRING),

    LIST_NOTIFICATIONS("listNotifications", false),
    CREATE_NOTIFICATION("createNotification", false, STRING, NUMBER, OPTIONAL_NUMBER),
    DELETE_NOTIFICATION("deleteNotification", false, STRING),

    DELETE_LAST_OPERATION("deleteLastOperation", false, STRING),
    EDIT_LAST_OPERATION("editLastOperation", false, STRING, OPTIONAL_NUMBER, STRING, STRING),

    ASK_FINANCE(
        "askFinance", true,
        STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING,
        OPTIONAL_NUMBER, OPTIONAL_NUMBER, OPTIONAL_NUMBER,
    ),

    ADD_LOAN("addLoan", true, STRING, STRING, NUMBER, STRING, STRING),
    REPAY_LOAN("repayLoan", true, STRING, STRING, OPTIONAL_NUMBER);

    val method: Method get() = BotFunctions::class.java.getMethod(functionName, *parameterTypes)

    companion object {
        fun from(name: String?): GeminiFunction? =
            entries.firstOrNull { it.functionName == name }
    }
}
