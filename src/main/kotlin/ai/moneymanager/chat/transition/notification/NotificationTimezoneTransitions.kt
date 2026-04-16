package ai.moneymanager.chat.transition.notification

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

val TIMEZONE_MAP = mapOf(
    "UTC+5 Астана" to "Asia/Almaty",
    "UTC+3 Москва" to "Europe/Moscow",
    "UTC+6 Омск" to "Asia/Omsk",
    "UTC+4 Дубай" to "Asia/Dubai",
    "UTC+2 Киев" to "Europe/Kiev",
    "UTC+1 Берлин" to "Europe/Berlin",
    // Extended
    "UTC+0 Лондон" to "Europe/London",
    "UTC-5 Нью-Йорк" to "America/New_York",
    "UTC+7 Бангкок" to "Asia/Bangkok",
    "UTC+8 Пекин" to "Asia/Shanghai",
    "UTC+9 Токио" to "Asia/Tokyo",
    "UTC+10 Сидней" to "Australia/Sydney",
    "UTC+12 Окленд" to "Pacific/Auckland"
)

val POPULAR_TIMEZONES = listOf(
    "UTC+5 Астана",
    "UTC+3 Москва",
    "UTC+6 Омск",
    "UTC+4 Дубай",
    "UTC+2 Киев",
    "UTC+1 Берлин"
)

val EXTENDED_TIMEZONES = TIMEZONE_MAP.keys.toList()

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.notificationTimezoneTransitions(
    userInfoService: UserInfoService,
    notificationService: NotificationService
) {
    // MENU -> check timezone -> NOTIFICATION_TIMEZONE_SELECT or NOTIFICATION_LIST
    transition {
        name = "Open notifications (need timezone)"
        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.NOTIFICATIONS
            guard { context.userInfo?.timezone == null }
        }
        then {
            to = MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT
        }
    }

    transition {
        name = "Open notifications (has timezone)"
        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.NOTIFICATIONS
            guard { context.userInfo?.timezone != null }
        }
        action {
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Select popular timezone
    transition {
        name = "Select popular timezone"
        condition {
            from = MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT
            button = MoneyManagerButtonType.TIMEZONE_ITEM
        }
        action {
            val tzId = TIMEZONE_MAP[buttonText]
            if (tzId != null) {
                val updated = userInfoService.updateTimezone(user.id, tzId)
                context.userInfo = updated
            }
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Show extended timezones
    simpleTransition(
        "Show extended timezones",
        MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT,
        MoneyManagerButtonType.TIMEZONE_OTHER,
        MoneyManagerState.NOTIFICATION_TIMEZONE_EXTENDED
    )

    // Select extended timezone
    transition {
        name = "Select extended timezone"
        condition {
            from = MoneyManagerState.NOTIFICATION_TIMEZONE_EXTENDED
            button = MoneyManagerButtonType.TIMEZONE_ITEM
        }
        action {
            val tzId = TIMEZONE_MAP[buttonText]
            if (tzId != null) {
                val updated = userInfoService.updateTimezone(user.id, tzId)
                context.userInfo = updated
            }
            context.notifications = notificationService.getNotifications(user.id)
        }
        then {
            to = MoneyManagerState.NOTIFICATION_LIST
        }
    }

    // Back from timezone screens
    simpleTransition(
        "Back from timezone select",
        MoneyManagerState.NOTIFICATION_TIMEZONE_SELECT,
        MoneyManagerButtonType.BACK_TO_MENU,
        MoneyManagerState.MENU
    )
    simpleTransition(
        "Back from extended timezone",
        MoneyManagerState.NOTIFICATION_TIMEZONE_EXTENDED,
        MoneyManagerButtonType.BACK_TO_MENU,
        MoneyManagerState.MENU
    )
}
