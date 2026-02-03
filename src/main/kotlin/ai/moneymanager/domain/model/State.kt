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

    // -------- NOTIFICATIONS --------
    NOTIFICATION_LIST,
    NOTIFICATION_CREATE_ENTER_NAME,
    NOTIFICATION_CREATE_SELECT_PERIOD,
    NOTIFICATION_CREATE_CUSTOM_ENTER_N,
    NOTIFICATION_CREATE_CUSTOM_SELECT_UNIT,
    NOTIFICATION_CREATE_SELECT_DAY_OF_WEEK,
    NOTIFICATION_CREATE_ENTER_DAY_OF_MONTH,
    NOTIFICATION_CREATE_SELECT_MONTH,
    NOTIFICATION_CREATE_ENTER_TIME,
    NOTIFICATION_CREATE_CONFIRM,
    NOTIFICATION_EDIT_SELECT,
    NOTIFICATION_EDIT_MENU,
    NOTIFICATION_EDIT_ENTER_NAME,
    NOTIFICATION_EDIT_SELECT_PERIOD,
    NOTIFICATION_EDIT_CUSTOM_ENTER_N,
    NOTIFICATION_EDIT_CUSTOM_SELECT_UNIT,
    NOTIFICATION_EDIT_SELECT_DAY_OF_WEEK,
    NOTIFICATION_EDIT_ENTER_DAY_OF_MONTH,
    NOTIFICATION_EDIT_SELECT_MONTH,
    NOTIFICATION_EDIT_ENTER_TIME,
    NOTIFICATION_EDIT_CONFIRM,
    NOTIFICATION_DELETE_SELECT,
    NOTIFICATION_DELETE_CONFIRM,
    NOTIFICATION_DELETE_ALL_CONFIRM,
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

    // Notifications
    NOTIFICATIONS,
    CREATE_NOTIFICATION,
    NOTIFICATION_ITEM,
    EDIT_NOTIFICATION,
    DELETE_NOTIFICATION,
    DELETE_ALL_NOTIFICATIONS,
    EDIT_NOTIFICATION_NAME,
    EDIT_NOTIFICATION_PERIOD,

    // Notification periods
    PERIOD_DAILY,
    PERIOD_WEEKLY,
    PERIOD_BIWEEKLY,
    PERIOD_MONTHLY,
    PERIOD_BIMONTHLY,
    PERIOD_YEARLY,
    PERIOD_CUSTOM,

    // Custom unit
    CUSTOM_UNIT_DAYS,
    CUSTOM_UNIT_WEEKS,
    CUSTOM_UNIT_MONTHS,
    CUSTOM_UNIT_YEARS,

    // Days of week
    DAY_MONDAY,
    DAY_TUESDAY,
    DAY_WEDNESDAY,
    DAY_THURSDAY,
    DAY_FRIDAY,
    DAY_SATURDAY,
    DAY_SUNDAY,

    // Months
    MONTH_JANUARY,
    MONTH_FEBRUARY,
    MONTH_MARCH,
    MONTH_APRIL,
    MONTH_MAY,
    MONTH_JUNE,
    MONTH_JULY,
    MONTH_AUGUST,
    MONTH_SEPTEMBER,
    MONTH_OCTOBER,
    MONTH_NOVEMBER,
    MONTH_DECEMBER,

    // Confirm
    CONFIRM_CREATE_NOTIFICATION,
    CONFIRM_EDIT_NOTIFICATION,
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
    var nlpResponse: String? = null
    var nlpTargetState: MoneyManagerState? = null
    var parsedCommand: BotCommand? = null

    // -------- NOTIFICATIONS --------
    var notifications: List<Notification> = emptyList()
    var currentNotification: Notification? = null
    var notificationTitleInput: String? = null
    var notificationPeriodTypeInput: NotificationPeriodType? = null
    var notificationCustomNInput: Int? = null
    var notificationCustomUnitInput: NotificationCustomUnit? = null
    var notificationDayOfWeekInput: Int? = null
    var notificationDayOfMonthInput: Int? = null
    var notificationMonthInput: Int? = null
    var notificationHourInput: Int? = null
    var notificationMinuteInput: Int? = null
    var isEditingNotification: Boolean = false
}
