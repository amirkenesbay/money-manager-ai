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

    // -------- FINANCE OPERATION --------
    FINANCE_OPERATION_EXPENSE_OR_INCOME,
    FINANCE_OPERATION_CREATE_EXPENSE,
    FINANCE_OPERATION_CREATE_INCOME,
}

enum class MoneyManagerButtonType {
    // Menu
    PERSONAL_ACCOUNTING,
    SHARED_ACCOUNTING,
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
    QUICK_GROUP_YOU_OWN_NAME,

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

    // Actions
    CONFIRM_JOIN,
    CONFIRM_DELETE,
    CANCEL,
    BACK_TO_MENU,

    // NLP
    CONFIRM_NLP_ACTION,

    // AI hints
    WHAT_TO_ASK,

    // FINANCE OPERATION
    FINANCE_OPERATION_EXPENSE_OR_INCOME,
    FINANCE_OPERATION_CREATE_EXPENSE_BUTTON,
    FINANCE_OPERATION_CREATE_INCOME_BUTTON
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

    // -------- NLP --------
    var nlpGroupName: String? = null
    var nlpGroupToDelete: MoneyGroup? = null
    var nlpResponse: String? = null
    var nlpNewMessage: Boolean = true
    var nlpTargetState: MoneyManagerState? = null
    var parsedCommand: BotCommand? = null
}