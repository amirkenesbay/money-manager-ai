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
    GROUP_INVITE_SELECT,
    GROUP_JOIN_CONFIRM,
    GROUP_LIST,
    GROUP_MEMBERS,
    GROUP_SWITCH,
    GROUP_EDIT_SELECT,
    GROUP_EDIT_ENTER_NAME,
    GROUP_DELETE_SELECT,
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
    CATEGORY_LIST,
    CATEGORY_ACTIONS,
    CATEGORY_EDIT_NAME,
    CATEGORY_DELETE_CONFIRM,
    CATEGORY_DELETE_ALL_CONFIRM,
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
    DELETE_CATEGORY_BUTTON,
    DELETE_ALL_CATEGORIES,

    // Actions
    CONFIRM_JOIN,
    CONFIRM_DELETE,
    CANCEL,
    BACK_TO_MENU,

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
    var isQuickGroupCreation: Boolean = false
    var categoriesCountToDelete: Int = 0

    // -------- CATEGORIES --------
    var categoryNameInput: String? = null
    var categoryIconInput: String? = null
    var categoryTypeInput: CategoryType? = null
    var currentCategory: Category? = null
    var categories: List<Category> = emptyList()

    // -------- NLP --------
    var nlpGroupName: String? = null
    var nlpGroupToDelete: MoneyGroup? = null
    var nlpResponse: String? = null
    var nlpNewMessage: Boolean = true
    var nlpTargetState: MoneyManagerState? = null
    var parsedCommand: BotCommand? = null
}