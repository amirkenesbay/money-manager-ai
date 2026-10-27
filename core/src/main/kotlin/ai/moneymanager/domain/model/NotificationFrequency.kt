package ai.moneymanager.domain.model

const val MAX_NOTIFICATION_NAME_LENGTH = 100
const val DEFAULT_NOTIFICATION_ICON = "🔔"

enum class ScheduleAnchor {
    NONE,
    DAY_OF_WEEK,
    DAY_OF_MONTH,
    DAY_OF_YEAR
}

enum class FrequencyType(val anchor: ScheduleAnchor, val maxCustomInterval: Int? = null) {
    DAILY(ScheduleAnchor.NONE),
    WEEKLY(ScheduleAnchor.DAY_OF_WEEK),
    BIWEEKLY(ScheduleAnchor.DAY_OF_WEEK),
    MONTHLY(ScheduleAnchor.DAY_OF_MONTH),
    BIMONTHLY(ScheduleAnchor.DAY_OF_MONTH),
    YEARLY(ScheduleAnchor.DAY_OF_YEAR),
    CUSTOM_DAYS(ScheduleAnchor.NONE, 365),
    CUSTOM_WEEKS(ScheduleAnchor.DAY_OF_WEEK, 52),
    CUSTOM_MONTHS(ScheduleAnchor.DAY_OF_MONTH, 24),
    CUSTOM_YEARS(ScheduleAnchor.DAY_OF_YEAR, 10)
}
