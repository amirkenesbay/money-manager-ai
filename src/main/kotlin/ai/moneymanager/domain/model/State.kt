package ai.moneymanager.domain.model

import ai.moneymanager.domain.model.nlp.BotCommand
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.BINARY)
enum class MoneyManagerState {
    // -------- MENU --------
    STARTED,
    MENU,

    // -------- GROUPS --------
    GROUP_MANAGEMENT,
    GROUP_CREATE_ENTER_NAME,
    GROUP_INVITE_SHOW,
    GROUP_JOIN_CONFIRM,
    GROUP_LIST,
    GROUP_ACTIONS,
    GROUP_MEMBERS,
    GROUP_EDIT_ENTER_NAME,
    GROUP_DELETE_CONFIRM,

    // -------- FINANCE --------
    FINANCE_MANAGEMENT,
    FINANCE_SELECT_CATEGORY,
    FINANCE_NO_CATEGORIES_WARNING,
    FINANCE_ENTER_AMOUNT,
    FINANCE_ENTER_COMMENT,
    FINANCE_SELECT_DATE,
    FINANCE_CALENDAR,
    FINANCE_SELECT_YEAR,
    FINANCE_SELECT_MONTH,
    FINANCE_OPERATION_SAVED,

    // -------- FINANCE REPORT --------
    FINANCE_REPORT_MENU,
    FINANCE_REPORT_COMPARISON,
    FINANCE_REPORT_ANALYTICS,
    FINANCE_REPORT_MEMBERS,
    FINANCE_REPORT_CATEGORY_SELECT,
    FINANCE_REPORT_CATEGORY_VIEW,

    // -------- FINANCE HISTORY --------
    FINANCE_HISTORY_VIEW,
    FINANCE_HISTORY_SELECT_START_DATE,
    FINANCE_HISTORY_SELECT_END_DATE,
    FINANCE_HISTORY_CALENDAR,
    FINANCE_HISTORY_SELECT_YEAR,
    FINANCE_HISTORY_SELECT_MONTH,

    // -------- NLP --------
    NLP_CONFIRM_CREATE_GROUP,
    NLP_CONFIRM_DELETE_GROUP,
    NLP_RESPONSE,

    // -------- CATEGORIES --------
    CATEGORY_MANAGEMENT,
    CATEGORY_NO_GROUP_WARNING,
    CATEGORY_CREATE_SELECT_TYPE,
    CATEGORY_CREATE_ENTER_NAME,
    CATEGORY_CREATE_RESULT,
    CATEGORY_LIST_SELECT_TYPE,
    CATEGORY_LIST,
    CATEGORY_ACTIONS,
    CATEGORY_EDIT_NAME,
    CATEGORY_EDIT_ICON,
    CATEGORY_DELETE_CONFIRM,
    CATEGORY_DELETE_ALL_CONFIRM,
}

enum class MoneyManagerButtonType {
    // Menu
    PERSONAL_ACCOUNTING,
    SHARED_ACCOUNTING,
    BALANCE,        // TODO: показ баланса группы
    NOTIFICATIONS,  // TODO: настройки уведомлений
    AI_ASSISTANT,   // TODO: AI ассистент
    FINANCE,        // экран "Финансы"
    ADD_EXPENSE,
    ADD_INCOME,

    // Groups
    CREATE_GROUP,
    MY_GROUPS,
    GROUP_ITEM,
    GROUP_MEMBERS,
    SWITCH_GROUP,
    EDIT_GROUP,
    DELETE_GROUP,
    INVITE_TO_GROUP,

    // Quick group names
    QUICK_GROUP_FAMILY,
    QUICK_GROUP_WITH_FRIEND,
    QUICK_GROUP_WORK,
    QUICK_GROUP_TRIP,

    // Categories
    CATEGORIES,
    CREATE_CATEGORY,
    MY_CATEGORIES,
    CATEGORY_TYPE_EXPENSE,
    CATEGORY_TYPE_INCOME,
    CATEGORY_ITEM,
    EDIT_CATEGORY,
    EDIT_CATEGORY_ICON,
    DELETE_CATEGORY_BUTTON,
    DELETE_ALL_CATEGORIES,

    // Quick category names - Expense
    QUICK_CATEGORY_FOOD_OUT,
    QUICK_CATEGORY_UTILITIES,
    QUICK_CATEGORY_MEDICINE,
    QUICK_CATEGORY_ENTERTAINMENT,
    QUICK_CATEGORY_CLOTHES,
    QUICK_CATEGORY_TAXI,

    // Quick category names - Income
    QUICK_CATEGORY_SALARY,
    QUICK_CATEGORY_BONUS,
    QUICK_CATEGORY_GIFT,
    QUICK_CATEGORY_FREELANCE,
    QUICK_CATEGORY_INVESTMENTS,
    QUICK_CATEGORY_DEBT_RETURN,

    // Finance
    FINANCE_ADD_EXPENSE,
    FINANCE_ADD_INCOME,
    FINANCE_CATEGORY_ITEM,
    CREATE_CATEGORY_FROM_FINANCE,
    FINANCE_HISTORY,
    FINANCE_REPORT,

    // Report
    REPORT_COMPARISON,
    REPORT_ANALYTICS,
    REPORT_BY_MEMBERS,
    REPORT_BY_CATEGORY,
    REPORT_PREV,
    REPORT_NEXT,
    REPORT_CATEGORY_ITEM,
    BACK_TO_REPORT_MENU,

    // History period
    CHANGE_PERIOD,
    QUICK_PERIOD_THIS_MONTH,
    QUICK_PERIOD_LAST_MONTH,
    QUICK_PERIOD_THIS_YEAR,
    BACK_TO_HISTORY,

    // Quick dates
    QUICK_DATE_TODAY,
    QUICK_DATE_YESTERDAY,
    QUICK_DATE_BEFORE_YESTERDAY,
    OPEN_CALENDAR,
    SKIP_COMMENT,
    BACK_TO_AMOUNT,

    // Actions
    ENTER_CUSTOM_NAME,
    CONFIRM_JOIN,
    CONFIRM_DELETE,
    CANCEL,
    BACK_TO_MENU,
    BACK_TO_FINANCE,
    BACK_TO_FINANCE_DATE,
    BACK_TO_CALENDAR,

    // NLP
    CONFIRM_NLP_ACTION,

    // AI hints
    WHAT_TO_ASK,
}

class MoneyManagerContext {
    // -------- USER --------
    var userInfo: UserInfo? = null

    // -------- GROUPS --------
    var pendingInviteToken: String? = null
    var pendingGroup: MoneyGroup? = null
    var pendingGroupOwnerInfo: UserInfo? = null
    var currentGroup: MoneyGroup? = null
    var groupNameInput: String? = null
    var userGroups: List<MoneyGroup> = emptyList()
    var groupMembersList: List<UserInfo> = emptyList()
    var isQuickGroupCreation: Boolean = false
    var inviteFromActions: Boolean = false
    var categoriesCountToDelete: Int = 0

    // -------- CATEGORIES --------
    var activeGroupName: String? = null
    var categoryNameInput: String? = null
    var categoryIconInput: String? = null
    var categoryTypeInput: CategoryType? = null
    var currentCategory: Category? = null
    var categories: List<Category> = emptyList()
    var isQuickCategoryCreation: Boolean = false

    // -------- UI FEEDBACK --------
    var renameConfirmation: String? = null
    var textInputResponse: Boolean = false
    var iconInputError: Boolean = false
    var groupNameDuplicateError: Boolean = false

    // -------- SESSION --------
    // Set to true on /start so that stale old dialog instances (which remain in MongoDB)
    // cannot accidentally match NLP text transitions.
    var isActive: Boolean = false

    // -------- INPUT MODE --------
    var manualTextInputActive: Boolean = false
    var customNameInputMode: Boolean = false

    // -------- FINANCE --------
    var financeOperationType: CategoryType? = null
    var selectedCategory: Category? = null
    var financeAmount: Double? = null
    var financeComment: String? = null
    var amountInputError: Boolean = false

    // -------- CALENDAR --------
    var calendarYear: Int = java.time.LocalDate.now().year
    var calendarMonth: Int = java.time.LocalDate.now().monthValue
    var selectedDate: java.time.LocalDate? = null

    fun clearFinanceData() {
        financeOperationType = null
        selectedCategory = null
        financeAmount = null
        financeComment = null
        selectedDate = null
    }

    // -------- REPORT --------
    var reportMonth: java.time.LocalDate? = null
    var reportText: String? = null
    var reportCategory: Category? = null

    // -------- HISTORY --------
    var historyStartDate: java.time.LocalDate? = null
    var historyEndDate: java.time.LocalDate? = null
    var historyReport: String? = null
    var historySelectingStart: Boolean = true

    // -------- NLP --------
    var nlpGroupName: String? = null
    var nlpGroupToDelete: MoneyGroup? = null
    var nlpResponse: String? = null
    var nlpNewMessage: Boolean = true
    var nlpTargetState: MoneyManagerState? = null
    var parsedCommand: BotCommand? = null
}