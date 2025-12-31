package ai.moneymanager.domain.model

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
    GROUP_MEMBERS,
    GROUP_SWITCH,
    GROUP_EDIT_SELECT,
    GROUP_EDIT_ENTER_NAME,
    GROUP_DELETE_SELECT,
    GROUP_DELETE_CONFIRM,
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

    // Quick group names
    QUICK_GROUP_FAMILY,
    QUICK_GROUP_WITH_FRIEND,
    QUICK_GROUP_WORK,
    QUICK_GROUP_TRIP,

    // Actions
    CONFIRM_JOIN,
    CONFIRM_DELETE,
    CANCEL,
    BACK_TO_MENU,
}

class MoneyManagerContext {
    // -------- USER --------
    var userInfo: UserInfo? = null

    // -------- GROUPS --------
    var pendingInviteToken: String? = null
    var pendingGroup: MoneyGroup? = null
    var currentGroup: MoneyGroup? = null
    var groupNameInput: String? = null
    var userGroups: List<MoneyGroup> = emptyList()
    var isQuickGroupCreation: Boolean = false
}